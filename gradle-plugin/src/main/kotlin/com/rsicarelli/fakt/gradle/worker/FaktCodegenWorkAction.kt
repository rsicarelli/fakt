// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.compiler.api.EmitPhase
import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import java.io.File
import java.util.Base64
import kotlinx.serialization.json.Json
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

/**
 * Inputs to [FaktCodegenWorkAction]. All Gradle managed property types — raw `Project`,
 * `Configuration`, or `SourceDirectorySet` references would be forbidden under the configuration
 * cache.
 */
internal interface FaktCodegenWorkParameters : WorkParameters {
    val sources: ConfigurableFileCollection
    val analysisOnlySources: ConfigurableFileCollection
    val commonSources: ConfigurableFileCollection
    val compileClasspath: ConfigurableFileCollection
    val commonKlibClasspath: ConfigurableFileCollection
    val faktCompilerClasspath: ConfigurableFileCollection
    val sourceSetContextJson: Property<String>
    val faktVersion: Property<String>
    val logLevel: Property<LogLevel>
    val enableCallHistory: Property<Boolean>
    val enableMutableFakes: Property<Boolean>
    val imports: ListProperty<String>
    val wasmTarget: Property<String>
    val commonFirMetadata: RegularFileProperty
    val generatedKotlinDir: DirectoryProperty
    val commonGeneratedKotlinDir: DirectoryProperty
    val firMetadataFile: RegularFileProperty
    val scratchDir: DirectoryProperty
    /** #152 spike: K2Native `-target` (e.g. `linux_x64`). */
    val konanTarget: Property<String>
    /** #152 spike: K/N distribution root, for the stdlib of shared-native compilations. */
    val konanHome: DirectoryProperty
    /** #152 spike: a shared-native metadata compilation (explicit commonized klibs). */
    val sharedNative: Property<Boolean>
    /** #152 spike (minimal #165 slice): `-opt-in` annotations of the compilation. */
    val optIns: ListProperty<String>
    /** #152 spike: `-Xrefines-paths` klibs of a shared-native compilation. */
    val refinesKlibs: ConfigurableFileCollection
}

/** Default `walkTopDown` cap for source discovery — covers typical Gradle source-set nesting. */
private const val DEFAULT_KOTLIN_SOURCE_DEPTH = 8

private const val MODULE_NAME = "fakt-analysis"
private const val EXIT_CODE_OK = "OK"

/**
 * Worker entry point: drives a compiler front door from `kotlin-compiler-embeddable` with the Fakt
 * `:compiler` shadowJar attached as a `-Xplugin`, writing generated `.kt` files into
 * [FaktCodegenWorkParameters.generatedKotlinDir].
 *
 * Three drivers (see [CompilerDriver]):
 * - **`KotlinMetadataCompiler`** for common (`commonMain`) producers — frontend-only, so unpaired
 *   `expect` declarations cannot fail the run, and generation happens at the FIR phase
 *   ([EmitPhase.FIR], the pipeline has no IR phase). Dependencies come from
 *   [FaktCodegenWorkParameters.commonKlibClasspath] (metadata klibs).
 * - **`K2JVMCompiler`** for everything else (JVM/Android classpaths). Generation still happens at
 *   the FIR phase — one emitter for every worker path — and the plugin's IR extension early-returns
 *   ([EmitPhase.FIR] parity with IR emission is locked by `FirIrEmissionParityTest`).
 * - **`K2JSCompiler`** for Kotlin/JS and Kotlin/Wasm platform compilations. Dependencies are the
 *   compilation's platform klibs (routed through the same klib input as the metadata driver); the
 *   run stops at klib serialization into [FaktCodegenWorkParameters.scratchDir], and generation
 *   happens at the FIR phase exactly as on the other drivers.
 *
 * Producer mode (no `commonFirMetadata` input) additionally instructs the plugin to write a
 * serialized `FirMetadataCache` to `firMetadataFile` so platform compilations downstream can skip
 * redundant FIR analysis. The plugin's `MetadataCacheManager` does the actual write — this action
 * only forwards the path.
 *
 * No `kotlin-compiler-embeddable` types appear in this class's signatures; everything reflective
 * lives behind [K2CompilerBridge].
 */
internal abstract class FaktCodegenWorkAction : WorkAction<FaktCodegenWorkParameters> {

    override fun execute() {
        val params = parameters
        // Clear stale outputs from previous runs: the dirs are task-owned (see resetDirectory).
        val outputDir = params.generatedKotlinDir.asFile.get().also(::resetDirectory)
        params.commonGeneratedKotlinDir.orNull?.asFile?.let(::resetDirectory)
        val analysisOnlyFiles = collectKotlinSources(params.analysisOnlySources.files)
        val commonFiles = collectKotlinSources(params.commonSources.files)
        require(commonFiles.isEmpty() || params.commonGeneratedKotlinDir.isPresent) {
            "commonSources require commonGeneratedKotlinDir: their fakes need a declared output."
        }
        val sourceSetContext = populateSourceSetContext(params, analysisOnlyFiles.isNotEmpty())
        val pluginJars = resolvePluginJars(params)

        invokeK2(
            K2Invocation(
                driver = CompilerDriver.forPlatformType(sourceSetContext.platformType),
                sourceFiles =
                    collectKotlinSources(params.sources.files) + commonFiles + analysisOnlyFiles,
                commonFragmentFiles = analysisOnlyFiles + commonFiles,
                compileClasspath =
                    params.compileClasspath.files.toList() +
                        params.commonKlibClasspath.files.toList(),
                pluginJars = pluginJars,
                outputDir = outputDir,
                scratchOutputDir = params.scratchDir.asFile.get(),
                sourceSetContextBase64 = encodeContext(sourceSetContext),
                logLevel = params.logLevel.getOrElse(LogLevel.QUIET),
                enableCallHistory = params.enableCallHistory.getOrElse(true),
                enableMutableFakes = params.enableMutableFakes.getOrElse(false),
                wasmTarget = params.wasmTarget.orNull,
                konanTarget = params.konanTarget.orNull,
                konanHome = params.konanHome.orNull?.asFile,
                sharedNative = params.sharedNative.getOrElse(false),
                optIns = params.optIns.getOrElse(emptyList()),
                refinesKlibs = params.refinesKlibs.files.filter { it.exists() },
            )
        )
    }

    /**
     * Decode the caller-supplied [SourceSetContext], then overwrite every absolute-path field with
     * values read from Gradle file properties at execution time. The
     * [FaktCodegenWorkParameters.sourceSetContextJson] `@Input` therefore never carries
     * machine-specific paths in the cache key, which is what makes cross-directory build-cache hits
     * work (relocation canary).
     *
     * Every worker invocation additionally flips [SourceSetContext.emitPhase] to [EmitPhase.FIR]:
     * the metadata pipeline has no IR phase at all, and on the K2JVM driver the FIR emitter is the
     * single generation path (the plugin's IR extension early-returns), byte-parity-locked by
     * `FirIrEmissionParityTest`. Set at execution time (not in the stored `@Input` JSON) like the
     * other mutated fields — the legacy in-process path never sets it and keeps emitting at IR.
     *
     * When ancestor sources ride along for analysis only ([hasAnalysisOnlySources] — the
     * source-partitioned consumer with expect/actual or common-type references), emission is
     * restricted to the compilation's own source set: the common producer owns the ancestors'
     * fakes.
     *
     * When [FaktCodegenWorkParameters.commonGeneratedKotlinDir] is set (a single-target KMP
     * project), it becomes [SourceSetContext.commonOutputDirectory]: the common fragment's fakes
     * are routed there and the default source set's fakes to the main output directory.
     */
    private fun populateSourceSetContext(
        params: FaktCodegenWorkParameters,
        hasAnalysisOnlySources: Boolean,
    ): SourceSetContext {
        val storedContext =
            Json.decodeFromString(SourceSetContext.serializer(), params.sourceSetContextJson.get())
        val outputDirectory = params.generatedKotlinDir.asFile.get().absolutePath
        val isConsumerMode = params.commonFirMetadata.isPresent
        return storedContext.copy(
            outputDirectory = outputDirectory,
            commonTestOutputDirectory = outputDirectory,
            metadataOutputPath =
                if (!isConsumerMode) params.firMetadataFile.orNull?.asFile?.absolutePath else null,
            metadataCachePath =
                if (isConsumerMode) params.commonFirMetadata.asFile.get().absolutePath else null,
            emitPhase = EmitPhase.FIR,
            commonOutputDirectory = params.commonGeneratedKotlinDir.orNull?.asFile?.absolutePath,
            emitSourceSets =
                if (hasAnalysisOnlySources) listOf(storedContext.defaultSourceSet.name)
                else emptyList(),
        )
    }

    private fun resolvePluginJars(params: FaktCodegenWorkParameters): List<File> =
        params.faktCompilerClasspath.files
            .filter { it.isFile }
            .toList()
            .also {
                require(it.isNotEmpty()) {
                    "faktCompilerClasspath must include the :compiler plugin jar(s)."
                }
            }

    private fun encodeContext(context: SourceSetContext): String =
        Base64.getEncoder()
            .encodeToString(
                Json.encodeToString(SourceSetContext.serializer(), context).toByteArray()
            )

    /** Aggregates the compiler invocation parameters so [invokeK2] keeps a single-screen body. */
    private data class K2Invocation(
        val driver: CompilerDriver,
        val sourceFiles: List<File>,
        /** Sources compiled as the common fragment (`-Xcommon-sources`). */
        val commonFragmentFiles: List<File>,
        val compileClasspath: List<File>,
        val pluginJars: List<File>,
        val outputDir: File,
        val scratchOutputDir: File,
        val sourceSetContextBase64: String,
        val logLevel: LogLevel,
        val enableCallHistory: Boolean,
        val enableMutableFakes: Boolean,
        val wasmTarget: String?,
        val konanTarget: String?,
        val konanHome: File?,
        val sharedNative: Boolean,
        val optIns: List<String>,
        val refinesKlibs: List<File>,
    )

    private fun invokeK2(call: K2Invocation) {
        val bridge = K2CompilerBridge(javaClass.classLoader, call.driver)
        val args = bridge.newArgs()
        populateSourceArgs(bridge, args, call)
        when (call.driver) {
            CompilerDriver.JVM -> populateJvmOutputArgs(bridge, args, call)
            CompilerDriver.METADATA -> populateMetadataOutputArgs(bridge, args, call)
            CompilerDriver.JS -> populateJsOutputArgs(bridge, args, call)
            CompilerDriver.NATIVE -> populateNativeOutputArgs(bridge, args, call)
        }
        populatePluginArgs(bridge, args, call)
        if (call.optIns.isNotEmpty()) {
            bridge.setOnArgs(args, "setOptIn", Array<String>::class.java, call.optIns.toTypedArray())
        }

        val collector = bridge.newPrintingMessageCollector(System.err)
        val exitCode =
            bridge
                .execMethod()
                .invoke(bridge.newCompiler(), collector, bridge.servicesEmpty(), args)
        val exitCodeName = (exitCode as Enum<*>).name
        check(exitCodeName == EXIT_CODE_OK) {
            "Fakt analysis failed (${call.driver} exit=$exitCodeName). See messages above."
        }
    }

    private fun populateSourceArgs(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        bridge.setOnArgs(
            args,
            "setFreeArgs",
            List::class.java,
            call.sourceFiles.map { it.absolutePath },
        )
        if (call.driver == CompilerDriver.NATIVE) {
            populateNativeLibraries(bridge, args, call)
            bridge.setOnArgs(args, "setModuleName", String::class.java, MODULE_NAME)
            return
        }
        // JVM and metadata arguments take a `-classpath`; the JS/Wasm arguments have none and read
        // their klib dependencies from `-libraries` instead.
        val dependencies =
            call.compileClasspath.joinToString(File.pathSeparator) { it.absolutePath }
        val dependencySetter =
            if (call.driver == CompilerDriver.JS) "setLibraries" else "setClasspath"
        bridge.setOnArgs(args, dependencySetter, String::class.java, dependencies)
        bridge.setOnArgs(args, "setModuleName", String::class.java, MODULE_NAME)
    }

    private fun populateJvmOutputArgs(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        // K2JVM still needs a destination for `.class` output we never read. Route it to the
        // task's `@LocalState scratchDir` so it never enters the build cache.
        bridge.setOnArgs(
            args,
            "setDestination",
            String::class.java,
            call.scratchOutputDir.resolve("bytecode").also { it.mkdirs() }.absolutePath,
        )
        bridge.setOnArgs(args, "setNoStdlib", Boolean::class.javaPrimitiveType!!, true)
        bridge.setOnArgs(args, "setNoReflect", Boolean::class.javaPrimitiveType!!, true)
        // Leave the JDK on the compilation classpath — production sources reference
        // `java.io.Serializable`, `java.util.*`, etc. K2 needs JDK rt to resolve them.

        populateConsumerMultiplatformArgs(bridge, args, call.commonFragmentFiles)
    }

    private fun populateJsOutputArgs(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        // K2JS needs a klib destination we never read. Route it to the task's `@LocalState
        // scratchDir` so it never enters the build cache. `-Xir-produce-klib-dir` stops the
        // pipeline at klib serialization: no JS/Wasm code generation, no linking.
        bridge.setOnArgs(
            args,
            "setOutputDir",
            String::class.java,
            call.scratchOutputDir.resolve("klib").also { it.mkdirs() }.absolutePath,
        )
        bridge.setOnArgs(args, "setIrProduceKlibDir", Boolean::class.javaPrimitiveType!!, true)
        // Kotlin/Wasm rides the same `K2JSCompiler` front door KGP uses for `compileKotlinWasm*`;
        // the target flavour (`wasm-js` / `wasm-wasi`) must match the stdlib klib on `-libraries`.
        if (call.wasmTarget != null) {
            bridge.setOnArgs(args, "setWasm", Boolean::class.javaPrimitiveType!!, true)
            bridge.setOnArgs(args, "setWasmTarget", String::class.java, call.wasmTarget)
        }
        populateConsumerMultiplatformArgs(bridge, args, call.commonFragmentFiles)
    }

    /**
     * #152 spike. K2Native takes repeated `-library` (a `String[]`). A leaf compilation keeps the
     * distribution's default stdlib + platform klibs; a shared-native metadata compilation passes
     * the commonized klibs explicitly (from its dependency files), so defaults are switched off and
     * the stdlib is added from the distribution, mirroring KGP's own arguments.
     */
    private fun populateNativeLibraries(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        val libraries = call.compileClasspath.map { it.absolutePath }.toMutableList()
        if (call.sharedNative) {
            val stdlib = requireNotNull(call.konanHome) { "konanHome is required" }
                .resolve("klib/common/stdlib").absolutePath
            if (libraries.none { it.endsWith("/klib/common/stdlib") }) libraries.add(0, stdlib)
            bridge.setOnArgs(args, "setNodefaultlibs", Boolean::class.javaPrimitiveType!!, true)
            bridge.setOnArgs(args, "setNostdlib", Boolean::class.javaPrimitiveType!!, true)
        }
        bridge.setOnArgs(args, "setLibraries", Array<String>::class.java, libraries.toTypedArray())
    }

    /**
     * #152 spike. `-produce library -Xmetadata-klib` stops K2Native after FIR + metadata
     * serialization (no IR, no LLVM, no `~/.konan/dependencies`), into the `@LocalState` scratch.
     */
    private fun populateNativeOutputArgs(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        bridge.setOnArgs(
            args,
            "setOutputName",
            String::class.java,
            call.scratchOutputDir.resolve("native-klib").also { it.mkdirs() }
                .resolve(MODULE_NAME).absolutePath,
        )
        bridge.setOnArgs(args, "setProduce", String::class.java, "library")
        bridge.setOnArgs(args, "setMetadataKlib", Boolean::class.javaPrimitiveType!!, true)
        bridge.setOnArgs(
            args,
            "setTarget",
            String::class.java,
            requireNotNull(call.konanTarget) { "konanTarget is required for Native" },
        )
        bridge.setOnArgs(args, "setMultiPlatform", Boolean::class.javaPrimitiveType!!, true)
        if (call.sharedNative) {
            if (call.refinesKlibs.isNotEmpty()) {
                bridge.setOnArgs(
                    args,
                    "setRefinesPaths",
                    Array<String>::class.java,
                    call.refinesKlibs.map { it.absolutePath }.toTypedArray(),
                )
            }
            bridge.setOnArgs(
                args,
                "setCommonSources",
                Array<String>::class.java,
                call.sourceFiles.map { it.absolutePath }.toTypedArray(),
            )
        } else {
            populateConsumerMultiplatformArgs(bridge, args, call.commonFragmentFiles)
        }
    }

    private fun populateMetadataOutputArgs(
        bridge: K2CompilerBridge,
        args: Any,
        call: K2Invocation,
    ) {
        // The metadata driver requires -d ("specify destination via -d") for the metadata klib we
        // never read. Route it to the task's `@LocalState scratchDir` so it never enters the
        // build cache. Note: K2MetadataCompilerArguments has no noStdlib/noReflect (JVM-only) —
        // the stdlib arrives as a metadata klib on the classpath.
        bridge.setOnArgs(
            args,
            "setDestination",
            String::class.java,
            call.scratchOutputDir.resolve("metadata-klib").also { it.mkdirs() }.absolutePath,
        )
        // Redundant on 2.3.x (the metadata configurator force-enables MultiPlatformProjects) but
        // kept defensively for user-overridden `faktWorker` compiler versions.
        bridge.setOnArgs(args, "setMultiPlatform", Boolean::class.javaPrimitiveType!!, true)
        // Mutes the "expect/actual classes are in Beta" warning — nothing else. Unpaired expects
        // are safe here regardless: this pipeline has no IR actualizer, the sole origin of
        // NO_ACTUAL_FOR_EXPECT.
        bridge.setOnArgs(args, "setExpectActualClasses", Boolean::class.javaPrimitiveType!!, true)
    }

    private fun populatePluginArgs(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        bridge.setOnArgs(
            args,
            "setPluginClasspaths",
            Array<String>::class.java,
            call.pluginJars.map { it.absolutePath }.toTypedArray(),
        )
        val pluginOptions =
            FaktPluginOptions.payload(
                logLevel = call.logLevel.name,
                outputDir = call.outputDir.absolutePath,
                sourceSetContextBase64 = call.sourceSetContextBase64,
                enableCallHistory = call.enableCallHistory,
                enableMutableFakes = call.enableMutableFakes,
            )
        bridge.setOnArgs(
            args,
            "setPluginOptions",
            Array<String>::class.java,
            pluginOptions.toTypedArray(),
        )
    }
}

/**
 * A source-partitioned consumer rides ancestor sources along for analysis (and a single-target
 * producer emits them too): marking them `-Xcommon-sources` splits the module into common +
 * platform fragments so `actual` declarations pair with their `expect`s (otherwise the frontend
 * rejects the `actual` keyword outright, and ACTUAL_WITHOUT_EXPECT is an error even under
 * `-Xmulti-platform`). `-Xexpect-actual-classes` mutes the expect/actual-classes Beta warning —
 * nothing else. All three live on `CommonCompilerArguments`, so every platform driver shares this.
 */
private fun populateConsumerMultiplatformArgs(
    bridge: K2CompilerBridge,
    args: Any,
    commonFragmentFiles: List<File>,
) {
    if (commonFragmentFiles.isEmpty()) return
    bridge.setOnArgs(args, "setMultiPlatform", Boolean::class.javaPrimitiveType!!, true)
    bridge.setOnArgs(args, "setExpectActualClasses", Boolean::class.javaPrimitiveType!!, true)
    bridge.setOnArgs(
        args,
        "setCommonSources",
        Array<String>::class.java,
        commonFragmentFiles.map { it.absolutePath }.toTypedArray(),
    )
}

/** Kotlin source files under [roots], walking directories up to [maxDepth] levels deep. */
private fun collectKotlinSources(
    roots: Set<File>,
    maxDepth: Int = DEFAULT_KOTLIN_SOURCE_DEPTH,
): List<File> =
    roots.flatMap { root ->
        when {
            root.isFile && root.extension == "kt" -> listOf(root)
            root.isDirectory ->
                root
                    .walkTopDown()
                    .maxDepth(maxDepth)
                    .filter { it.isFile && it.extension == "kt" }
                    .toList()
            else -> emptyList()
        }
    }

/**
 * Clears a task-owned output directory before a run: a deleted `@Fake` source must not leave its
 * generated fake behind, and an in-process copy left in the same directory by a pre-upgrade build
 * is removed too.
 */
private fun resetDirectory(dir: File) {
    dir.deleteRecursively()
    dir.mkdirs()
}
