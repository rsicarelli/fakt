// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.gradle.FaktGradleSubplugin

/**
 * Parses the compilation's [arguments] into the freshly created [args] instance, BEFORE Fakt's own
 * setters run: every value Fakt owns (sources, classpath, destination, plugin options, ...) is set
 * afterwards and therefore always overwrites whatever the forwarded list said.
 *
 * @throws IllegalStateException naming the rejected arguments when the compiler cannot accept them
 */
internal fun applyForwardedArguments(bridge: K2CompilerBridge, args: Any, arguments: List<String>) {
    if (arguments.isEmpty()) return
    val problems = bridge.parseArguments(args, arguments)
    check(problems.isEmpty()) { forwardedArgumentsRejectedMessage(problems, arguments) }
}

/** Sets `-jdk-home` on JVM driver [args] when a toolchain JDK was forwarded. */
internal fun applyJdkHome(bridge: K2CompilerBridge, args: Any, jdkHome: String?) {
    if (jdkHome == null) return
    bridge.setOnArgs(args, "setJdkHome", String::class.java, jdkHome)
}

/** Failure message for [applyForwardedArguments]; pure so it is unit-testable. */
internal fun forwardedArgumentsRejectedMessage(
    problems: List<String>,
    arguments: List<String>,
): String =
    "Fakt: forwarded compiler arguments rejected: $problems (arguments: $arguments; " +
        "worker compiler ${FaktGradleSubplugin.FAKT_KOTLIN_VERSION}). If your Kotlin is newer " +
        "than the worker, see the faktWorker configuration."

/** Failure message for a K2 run that exited non-OK; mentions the forwarded arguments if any. */
internal fun analysisFailedMessage(
    driver: CompilerDriver,
    exitCodeName: String,
    arguments: List<String>,
): String =
    "Fakt analysis failed ($driver exit=$exitCodeName). See messages above." +
        if (arguments.isEmpty()) ""
        else
            " The forwarded compiler arguments were $arguments " +
                "(worker compiler ${FaktGradleSubplugin.FAKT_KOTLIN_VERSION})."
