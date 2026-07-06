// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import java.io.PrintStream
import java.lang.reflect.Method

/**
 * Fully-qualified names of every `kotlin-compiler-embeddable` symbol [FaktCodegenWorkAction]
 * touches. Centralised so a Kotlin compiler refactor that renames or moves any of these surfaces a
 * single failing constant rather than scattered `NoClassDefFoundError`s.
 */
internal object K2Fqns {
    const val K2_JVM_COMPILER = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    const val K2_JVM_COMPILER_ARGUMENTS =
        "org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments"
    const val KOTLIN_METADATA_COMPILER = "org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler"
    const val K2_METADATA_COMPILER_ARGUMENTS =
        "org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments"
    const val COMMON_COMPILER_ARGUMENTS =
        "org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments"
    const val MESSAGE_COLLECTOR = "org.jetbrains.kotlin.cli.common.messages.MessageCollector"
    const val PRINTING_MESSAGE_COLLECTOR =
        "org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector"
    const val MESSAGE_RENDERER = "org.jetbrains.kotlin.cli.common.messages.MessageRenderer"
    const val SERVICES = "org.jetbrains.kotlin.config.Services"
}

/**
 * The two compiler front doors the worker can drive, both shipped in `kotlin-compiler-embeddable`
 * and invoked through the identical `CLICompiler.exec(MessageCollector, Services,
 * CommonCompilerArguments)` surface.
 * - [JVM] — platform driver for JVM/Android classpaths (`.class`/jar dependencies). Runs the full
 *   FIR → fir2ir → backend pipeline.
 * - [METADATA] — the common (frontend-only) driver used for `commonMain` producers: FIR resolve +
 *   checkers → metadata-klib serialization. **No fir2ir and no IR actualizer**, which is what makes
 *   unpaired `expect` declarations structurally incapable of failing the run
 *   (`NO_ACTUAL_FOR_EXPECT` is an actualizer diagnostic). Dependencies must be **metadata klibs**;
 *   JVM jars on its classpath are ignored.
 */
internal enum class CompilerDriver(val compilerFqn: String, val argumentsFqn: String) {
    JVM(K2Fqns.K2_JVM_COMPILER, K2Fqns.K2_JVM_COMPILER_ARGUMENTS),
    METADATA(K2Fqns.KOTLIN_METADATA_COMPILER, K2Fqns.K2_METADATA_COMPILER_ARGUMENTS),
}

/**
 * Plugin-option contract Fakt's `:compiler` plugin understands. The producer-mode
 * `metadataOutputPath` lives inside [SourceSetContext] (read by `FaktOptions.metadataOutputPath`),
 * not as a separate CLI option — the worker mutates the context before encoding it.
 */
internal object FaktPluginOptions {
    const val PREFIX = "plugin:com.rsicarelli.fakt:"
    const val ENABLED = "${PREFIX}enabled"
    const val LOG_LEVEL = "${PREFIX}logLevel"
    const val OUTPUT_DIR = "${PREFIX}outputDir"
    const val SOURCE_SET_CONTEXT = "${PREFIX}sourceSetContext"
}

/**
 * Reflective handle on `kotlin-compiler-embeddable` types. One instance per worker invocation; each
 * `Class<*>` is resolved lazily once and reused, so the typical `K2JVMCompilerArguments` setter
 * chain pays the lookup cost a single time. All compiler types stay behind this surface so the
 * surrounding [FaktCodegenWorkAction] class file references no embeddable symbols — Gradle's class
 * inspector walks worker-action signatures during decoration and would choke otherwise.
 */
internal class K2CompilerBridge(
    private val cl: ClassLoader,
    private val driver: CompilerDriver = CompilerDriver.JVM,
) {
    private val compilerClass by lazy { load(driver.compilerFqn) }
    private val argumentsClass by lazy { load(driver.argumentsFqn) }
    private val commonArgumentsClass by lazy { load(K2Fqns.COMMON_COMPILER_ARGUMENTS) }
    private val messageCollectorClass by lazy { load(K2Fqns.MESSAGE_COLLECTOR) }
    private val printingMessageCollectorClass by lazy { load(K2Fqns.PRINTING_MESSAGE_COLLECTOR) }
    private val messageRendererClass by lazy { load(K2Fqns.MESSAGE_RENDERER) }
    private val servicesClass by lazy { load(K2Fqns.SERVICES) }

    fun newArgs(): Any = argumentsClass.getDeclaredConstructor().newInstance()

    fun newCompiler(): Any = compilerClass.getDeclaredConstructor().newInstance()

    fun newPrintingMessageCollector(out: PrintStream): Any =
        printingMessageCollectorClass
            .getConstructor(
                PrintStream::class.java,
                messageRendererClass,
                Boolean::class.javaPrimitiveType,
            )
            .newInstance(out, plainFullPathsRenderer(), false)

    fun servicesEmpty(): Any = servicesClass.getField("EMPTY").get(null)

    fun execMethod(): Method =
        compilerClass.getMethod("exec", messageCollectorClass, servicesClass, commonArgumentsClass)

    /**
     * Walk [argsInstance]'s class hierarchy looking for [setterName]; tolerates the setter living
     * either on `K2JVMCompilerArguments` or any of its `CommonCompilerArguments`/
     * `CommonToolArguments` ancestors (`freeArgs` lives on the latter).
     */
    fun setOnArgs(argsInstance: Any, setterName: String, paramType: Class<*>, value: Any?) {
        var clazz: Class<*>? = argsInstance.javaClass
        while (clazz != null) {
            try {
                val method = clazz.getDeclaredMethod(setterName, paramType)
                method.isAccessible = true
                method.invoke(argsInstance, value)
                return
            } catch (_: NoSuchMethodException) {
                clazz = clazz.superclass
            }
        }
        error(
            "Setter $setterName(${paramType.simpleName}) not found on ${argsInstance.javaClass.name}"
        )
    }

    private fun plainFullPathsRenderer(): Any =
        messageRendererClass.getField("PLAIN_FULL_PATHS").get(null)

    private fun load(fqn: String): Class<*> = Class.forName(fqn, true, cl)
}
