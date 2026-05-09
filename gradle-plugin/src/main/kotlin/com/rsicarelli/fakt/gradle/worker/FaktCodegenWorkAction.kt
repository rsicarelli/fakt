// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.compiler.api.FirMetadataCache
import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.fir.cache.MetadataCacheSerializer
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
 * Inputs to [FaktCodegenWorkAction]. All Gradle managed property types — never raw `Project`,
 * `Configuration`, or `SourceDirectorySet` references, which are forbidden under the configuration
 * cache (research artifact 1, R3).
 */
public interface FaktCodegenWorkParameters : WorkParameters {
    public val sources: ConfigurableFileCollection
    public val compileClasspath: ConfigurableFileCollection
    public val faktCompilerClasspath: ConfigurableFileCollection
    public val sourceSetContextJson: Property<String>
    public val faktVersion: Property<String>
    public val logLevel: Property<LogLevel>
    public val imports: ListProperty<String>
    public val commonFirMetadata: RegularFileProperty
    public val generatedKotlinDir: DirectoryProperty
    public val firMetadataFile: RegularFileProperty
    public val scratchDir: DirectoryProperty
}

/**
 * Worker entry point: invokes `K2JVMCompiler` with the Fakt `:compiler` shadowJar attached as a
 * `-Xplugin`. The plugin's existing `FaktCompilerPluginRegistrar` handles all FIR + IR work and
 * writes generated `.kt` files into [FaktCodegenWorkParameters.generatedKotlinDir] via the
 * `outputDir` plugin option — exactly the same code path KGP triggers today, just hosted in a
 * `@CacheableTask` Worker rather than as a side effect of `compileKotlin*`.
 *
 * Producer mode (no `commonFirMetadata` input) additionally writes a serialized [FirMetadataCache]
 * to `firMetadataFile` so platform compilations downstream can skip redundant FIR analysis.
 *
 * No types from `kotlin-compiler-embeddable` appear in this class's signatures — Gradle's class
 * inspector walks Worker-action signatures during decoration, and the daemon classpath deliberately
 * excludes the heavy compiler artifacts. The compiler classes live only on the isolated worker
 * classloader, accessed via reflection from inside [execute].
 */
public abstract class FaktCodegenWorkAction : WorkAction<FaktCodegenWorkParameters> {

    override fun execute() {
        val params = parameters
        val outputDir = params.generatedKotlinDir.asFile.get().also { it.mkdirs() }
        val sourceSetContext =
            Json.decodeFromString(SourceSetContext.serializer(), params.sourceSetContextJson.get())

        clearStaleOutputs(outputDir)

        val pluginJars =
            params.faktCompilerClasspath.files
                .filter { it.isFile }
                .toList()
                .also {
                    require(it.isNotEmpty()) {
                        "faktCompilerClasspath must include the :compiler plugin jar(s)."
                    }
                }
        invokeK2JvmCompiler(
            K2Invocation(
                sourceFiles = collectKotlinSources(params.sources.files),
                compileClasspath = params.compileClasspath.files.toList(),
                pluginJars = pluginJars,
                outputDir = outputDir,
                sourceSetContextBase64 = encodeContext(sourceSetContext),
                logLevel = params.logLevel.getOrElse(LogLevel.QUIET),
            )
        )

        params.firMetadataFile.orNull?.asFile?.let { metadataFile ->
            if (!params.commonFirMetadata.isPresent) {
                writeProducerCache(metadataFile, params.faktVersion.getOrElse("unknown"))
            }
        }
    }

    private fun collectKotlinSources(roots: Set<File>): List<File> =
        roots.flatMap { root ->
            when {
                root.isFile && root.extension == "kt" -> listOf(root)
                root.isDirectory ->
                    root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
                else -> emptyList()
            }
        }

    private fun encodeContext(context: SourceSetContext): String =
        Base64.getEncoder()
            .encodeToString(
                Json.encodeToString(SourceSetContext.serializer(), context).toByteArray()
            )

    /**
     * Aggregates the K2 invocation parameters so the reflective driver below stays under detekt's
     * function-size and parameter-count thresholds without sacrificing readability.
     */
    private data class K2Invocation(
        val sourceFiles: List<File>,
        val compileClasspath: List<File>,
        val pluginJars: List<File>,
        val outputDir: File,
        val sourceSetContextBase64: String,
        val logLevel: LogLevel,
    )

    /**
     * Reflective `K2JVMCompiler.exec` invocation. All compiler types stay inside this method's body
     * so the surrounding [WorkAction] class file never references them — without that, Gradle's
     * `ClassInspector` chokes during decoration because the daemon classpath doesn't carry
     * `kotlin-compiler-embeddable`.
     */
    private fun invokeK2JvmCompiler(call: K2Invocation) {
        val cl = javaClass.classLoader
        val compilerClass = Class.forName("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", true, cl)
        val argsClass =
            Class.forName(
                "org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments",
                true,
                cl,
            )
        val args = argsClass.getDeclaredConstructor().newInstance()
        populateK2Args(args, call)
        val collector = newMessageCollector(cl)
        val servicesClass = Class.forName("org.jetbrains.kotlin.config.Services", true, cl)
        val exec =
            compilerClass.getMethod(
                "exec",
                Class.forName(
                    "org.jetbrains.kotlin.cli.common.messages.MessageCollector",
                    true,
                    cl,
                ),
                servicesClass,
                Class.forName(
                    "org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments",
                    true,
                    cl,
                ),
            )
        val compiler = compilerClass.getDeclaredConstructor().newInstance()
        val exitCode =
            exec.invoke(compiler, collector, servicesClass.getField("EMPTY").get(null), args)
        val exitCodeName = (exitCode as Enum<*>).name
        check(exitCodeName == "OK") {
            "Fakt analysis failed (K2JVMCompiler exit=$exitCodeName). See messages above."
        }
    }

    private fun populateK2Args(args: Any, call: K2Invocation) {
        invokeSetter(
            args,
            "setFreeArgs",
            List::class.java,
            call.sourceFiles.map { it.absolutePath },
        )
        invokeSetter(
            args,
            "setClasspath",
            String::class.java,
            call.compileClasspath.joinToString(File.pathSeparator) { it.absolutePath },
        )
        invokeSetter(args, "setModuleName", String::class.java, "fakt-analysis")
        invokeSetter(
            args,
            "setDestination",
            String::class.java,
            File(call.outputDir, ".bytecode").also { it.mkdirs() }.absolutePath,
        )
        invokeSetter(args, "setNoStdlib", Boolean::class.javaPrimitiveType!!, true)
        invokeSetter(args, "setNoReflect", Boolean::class.javaPrimitiveType!!, true)
        invokeSetter(args, "setNoJdk", Boolean::class.javaPrimitiveType!!, true)
        invokeSetter(
            args,
            "setPluginClasspaths",
            Array<String>::class.java,
            call.pluginJars.map { it.absolutePath }.toTypedArray(),
        )
        invokeSetter(
            args,
            "setPluginOptions",
            Array<String>::class.java,
            arrayOf(
                "plugin:com.rsicarelli.fakt:enabled=true",
                "plugin:com.rsicarelli.fakt:logLevel=${call.logLevel.name}",
                "plugin:com.rsicarelli.fakt:outputDir=${call.outputDir.absolutePath}",
                "plugin:com.rsicarelli.fakt:sourceSetContext=${call.sourceSetContextBase64}",
            ),
        )
    }

    private fun newMessageCollector(cl: ClassLoader): Any {
        val messageCollectorClass =
            Class.forName(
                "org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector",
                true,
                cl,
            )
        val messageRendererClass =
            Class.forName("org.jetbrains.kotlin.cli.common.messages.MessageRenderer", true, cl)
        val plainFullPaths = messageRendererClass.getField("PLAIN_FULL_PATHS").get(null)
        return messageCollectorClass
            .getConstructor(
                java.io.PrintStream::class.java,
                messageRendererClass,
                Boolean::class.javaPrimitiveType,
            )
            .newInstance(System.err, plainFullPaths, false)
    }

    private fun invokeSetter(target: Any, name: String, paramType: Class<*>, value: Any?) {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val method = clazz.getDeclaredMethod(name, paramType)
                method.isAccessible = true
                method.invoke(target, value)
                return
            } catch (_: NoSuchMethodException) {
                clazz = clazz.superclass
            }
        }
        error("Setter $name(${paramType.simpleName}) not found on ${target.javaClass.name}")
    }

    private fun clearStaleOutputs(outputDir: File) {
        if (outputDir.exists()) {
            outputDir.walkBottomUp().filter { it != outputDir && it.isFile }.forEach { it.delete() }
        }
    }

    private fun writeProducerCache(metadataFile: File, faktVersion: String) {
        // Reset timestamp to keep the file byte-deterministic; non-deterministic outputs would
        // cause
        // spurious cache misses on consumer compilations across machines (research artifact 2, R5).
        val cache =
            FirMetadataCache(
                cacheSignature = "v$faktVersion",
                interfaces = emptyList(),
                classes = emptyList(),
                generatedAt = 0L,
            )
        metadataFile.parentFile?.mkdirs()
        MetadataCacheSerializer.serialize(cache, metadataFile.absolutePath)
    }
}
