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
    val compileClasspath: ConfigurableFileCollection
    val commonKlibClasspath: ConfigurableFileCollection
    val faktCompilerClasspath: ConfigurableFileCollection
    val sourceSetContextJson: Property<String>
    val faktVersion: Property<String>
    val logLevel: Property<LogLevel>
    val imports: ListProperty<String>
    val commonFirMetadata: RegularFileProperty
    val generatedKotlinDir: DirectoryProperty
    val firMetadataFile: RegularFileProperty
    val scratchDir: DirectoryProperty
}

/** Default `walkTopDown` cap for source discovery — covers typical Gradle source-set nesting. */
private const val DEFAULT_KOTLIN_SOURCE_DEPTH = 8

private const val MODULE_NAME = "fakt-analysis"
private const val EXIT_CODE_OK = "OK"
private const val COMMON_PLATFORM = "common"

/**
 * Worker entry point: drives a compiler front door from `kotlin-compiler-embeddable` with the Fakt
 * `:compiler` shadowJar attached as a `-Xplugin`, writing generated `.kt` files into
 * [FaktCodegenWorkParameters.generatedKotlinDir].
 *
 * Two drivers (see [CompilerDriver]):
 * - **`KotlinMetadataCompiler`** for common (`commonMain`) producers — frontend-only, so unpaired
 *   `expect` declarations cannot fail the run, and generation happens at the FIR phase
 *   ([EmitPhase.FIR], the pipeline has no IR phase). Dependencies come from
 *   [FaktCodegenWorkParameters.commonKlibClasspath] (metadata klibs).
 * - **`K2JVMCompiler`** for everything else (JVM/Android classpaths) — unchanged behavior, the
 *   plugin's IR extension writes the fakes.
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
        // Clear stale outputs from previous runs: the dir is task-owned, and a deleted @Fake
        // source must not leave its generated fake behind.
        val outputDir =
            params.generatedKotlinDir.asFile.get().also {
                it.deleteRecursively()
                it.mkdirs()
            }
        val sourceSetContext = populateSourceSetContext(params)
        val pluginJars = resolvePluginJars(params)
        val commonAnalysis =
            sourceSetContext.platformType.equals(COMMON_PLATFORM, ignoreCase = true)

        invokeK2(
            K2Invocation(
                driver = if (commonAnalysis) CompilerDriver.METADATA else CompilerDriver.JVM,
                sourceFiles = collectKotlinSources(params.sources.files),
                compileClasspath =
                    params.compileClasspath.files.toList() +
                        params.commonKlibClasspath.files.toList(),
                pluginJars = pluginJars,
                outputDir = outputDir,
                scratchOutputDir = params.scratchDir.asFile.get(),
                sourceSetContextBase64 = encodeContext(sourceSetContext),
                logLevel = params.logLevel.getOrElse(LogLevel.QUIET),
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
     * Common producers additionally flip [SourceSetContext.emitPhase] to [EmitPhase.FIR]: the
     * metadata pipeline has no IR phase, so the plugin's FIR checkers must write the fakes. Set at
     * execution time (not in the stored `@Input` JSON) like the other mutated fields.
     */
    private fun populateSourceSetContext(params: FaktCodegenWorkParameters): SourceSetContext {
        val storedContext =
            Json.decodeFromString(SourceSetContext.serializer(), params.sourceSetContextJson.get())
        val outputDirectory = params.generatedKotlinDir.asFile.get().absolutePath
        val isConsumerMode = params.commonFirMetadata.isPresent
        val commonAnalysis = storedContext.platformType.equals(COMMON_PLATFORM, ignoreCase = true)
        return storedContext.copy(
            outputDirectory = outputDirectory,
            commonTestOutputDirectory = outputDirectory,
            metadataOutputPath =
                if (!isConsumerMode) params.firMetadataFile.orNull?.asFile?.absolutePath else null,
            metadataCachePath =
                if (isConsumerMode) params.commonFirMetadata.asFile.get().absolutePath else null,
            emitPhase = if (commonAnalysis) EmitPhase.FIR else storedContext.emitPhase,
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

    private fun encodeContext(context: SourceSetContext): String =
        Base64.getEncoder()
            .encodeToString(
                Json.encodeToString(SourceSetContext.serializer(), context).toByteArray()
            )

    /** Aggregates the compiler invocation parameters so [invokeK2] keeps a single-screen body. */
    private data class K2Invocation(
        val driver: CompilerDriver,
        val sourceFiles: List<File>,
        val compileClasspath: List<File>,
        val pluginJars: List<File>,
        val outputDir: File,
        val scratchOutputDir: File,
        val sourceSetContextBase64: String,
        val logLevel: LogLevel,
    )

    private fun invokeK2(call: K2Invocation) {
        val bridge = K2CompilerBridge(javaClass.classLoader, call.driver)
        val args = bridge.newArgs()
        populateCompilerArgs(bridge, args, call)

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

    private fun populateCompilerArgs(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        populateSourceArgs(bridge, args, call)
        when (call.driver) {
            CompilerDriver.JVM -> populateJvmOutputArgs(bridge, args, call)
            CompilerDriver.METADATA -> populateMetadataOutputArgs(bridge, args, call)
        }
        populatePluginArgs(bridge, args, call)
    }

    private fun populateSourceArgs(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        bridge.setOnArgs(
            args,
            "setFreeArgs",
            List::class.java,
            call.sourceFiles.map { it.absolutePath },
        )
        bridge.setOnArgs(
            args,
            "setClasspath",
            String::class.java,
            call.compileClasspath.joinToString(File.pathSeparator) { it.absolutePath },
        )
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
            arrayOf(
                "${FaktPluginOptions.ENABLED}=true",
                "${FaktPluginOptions.LOG_LEVEL}=${call.logLevel.name}",
                "${FaktPluginOptions.OUTPUT_DIR}=${call.outputDir.absolutePath}",
                "${FaktPluginOptions.SOURCE_SET_CONTEXT}=${call.sourceSetContextBase64}",
            )
        bridge.setOnArgs(args, "setPluginOptions", Array<String>::class.java, pluginOptions)
    }
}
