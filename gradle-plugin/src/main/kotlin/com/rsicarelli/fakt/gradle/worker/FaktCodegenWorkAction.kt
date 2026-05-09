// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

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
 * cache (research artifact 1, R3).
 */
internal interface FaktCodegenWorkParameters : WorkParameters {
    val sources: ConfigurableFileCollection
    val compileClasspath: ConfigurableFileCollection
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

/**
 * Worker entry point: invokes `K2JVMCompiler` with the Fakt `:compiler` shadowJar attached as a
 * `-Xplugin`. The plugin's existing `FaktCompilerPluginRegistrar` handles all FIR + IR work and
 * writes generated `.kt` files into [FaktCodegenWorkParameters.generatedKotlinDir] via the
 * `outputDir` plugin option — same code path KGP triggers today, just hosted in a `@CacheableTask`
 * Worker rather than as a side effect of `compileKotlin*`.
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
        val outputDir = params.generatedKotlinDir.asFile.get().also { it.mkdirs() }
        val sourceSetContext = populateSourceSetContext(params)
        val pluginJars = resolvePluginJars(params)

        invokeK2(
            K2Invocation(
                sourceFiles = collectKotlinSources(params.sources.files),
                compileClasspath = params.compileClasspath.files.toList(),
                pluginJars = pluginJars,
                outputDir = outputDir,
                bytecodeDir = params.scratchDir.asFile.get().resolve("bytecode"),
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
     */
    private fun populateSourceSetContext(params: FaktCodegenWorkParameters): SourceSetContext {
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

    /** Aggregates the K2 invocation parameters so [invokeK2] keeps a single-screen body. */
    private data class K2Invocation(
        val sourceFiles: List<File>,
        val compileClasspath: List<File>,
        val pluginJars: List<File>,
        val outputDir: File,
        val bytecodeDir: File,
        val sourceSetContextBase64: String,
        val logLevel: LogLevel,
    )

    private fun invokeK2(call: K2Invocation) {
        val bridge = K2CompilerBridge(javaClass.classLoader)
        val args = bridge.newArgs()
        populateK2Args(bridge, args, call)

        val collector = bridge.newPrintingMessageCollector(System.err)
        val exitCode =
            bridge
                .execMethod()
                .invoke(bridge.newCompiler(), collector, bridge.servicesEmpty(), args)
        val exitCodeName = (exitCode as Enum<*>).name
        check(exitCodeName == EXIT_CODE_OK) {
            "Fakt analysis failed (K2JVMCompiler exit=$exitCodeName). See messages above."
        }
    }

    private fun populateK2Args(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        populateSourceArgs(bridge, args, call)
        populateOutputArgs(bridge, args, call)
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

    private fun populateOutputArgs(bridge: K2CompilerBridge, args: Any, call: K2Invocation) {
        // K2 still needs a destination for `.class` output we never read. Route it to the task's
        // `@LocalState scratchDir` so it never enters the build cache (audit MEDIUM #5).
        bridge.setOnArgs(
            args,
            "setDestination",
            String::class.java,
            call.bytecodeDir.also { it.mkdirs() }.absolutePath,
        )
        bridge.setOnArgs(args, "setNoStdlib", Boolean::class.javaPrimitiveType!!, true)
        bridge.setOnArgs(args, "setNoReflect", Boolean::class.javaPrimitiveType!!, true)
        bridge.setOnArgs(args, "setNoJdk", Boolean::class.javaPrimitiveType!!, true)
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
