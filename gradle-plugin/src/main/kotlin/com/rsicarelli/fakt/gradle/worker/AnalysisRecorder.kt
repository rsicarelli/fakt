// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.compiler.api.LogLevel
import java.lang.invoke.MethodHandles
import java.lang.reflect.Proxy

/** One message captured from the analysis run, already classified. */
internal data class RecordedDiagnostic(val message: String, val verdict: DiagnosticVerdict)

/** Collects the diagnostics of one K2 run and turns them into the worker's decision. */
internal class AnalysisRecorder {
    private val recorded = mutableListOf<RecordedDiagnostic>()

    /** Sink for `K2CompilerBridge.newRecordingCollector`. */
    fun record(severity: String, message: String, hasLocation: Boolean) {
        val verdict = classify(severity, message, hasLocation)
        if (verdict != DiagnosticVerdict.IGNORED) {
            synchronized(recorded) { recorded += RecordedDiagnostic(message, verdict) }
        }
    }

    fun tolerated(): List<String> = messages(DiagnosticVerdict.TOLERATED)

    fun fatal(): List<String> = messages(DiagnosticVerdict.FATAL)

    fun decide(exitName: String): AnalysisOutcome = outcome(exitName, fatal().size)

    private fun messages(verdict: DiagnosticVerdict): List<String> =
        synchronized(recorded) { recorded.filter { it.verdict == verdict }.map { it.message } }
}

/** Lines to log when a run succeeded despite tolerated errors; empty below INFO. */
internal fun toleratedLogLines(tolerated: List<String>, logLevel: LogLevel): List<String> =
    if (logLevel >= LogLevel.INFO) tolerated.map { "Fakt: tolerated compiler error: $it" }
    else emptyList()

/** [analysisFailedMessage] plus the fatal diagnostics the recorder saw. */
internal fun analysisFailedWithDiagnostics(
    driver: CompilerDriver,
    exitCodeName: String,
    arguments: List<String>,
    fatal: List<String>,
): String {
    val base = analysisFailedMessage(driver, exitCodeName, arguments)
    return if (fatal.isEmpty()) base
    else base + "\nFatal diagnostics:" + fatal.joinToString("") { "\n  - $it" }
}

private const val REPORT_ARITY = 3

/**
 * A `MessageCollector` proxy that forwards every call to [delegate] (console output and
 * `hasErrors()` stay exactly as the printing collector answers) and also hands each reported
 * message to [sink] as (severity name, message, has source location). No compiler types appear in
 * the signature.
 */
internal fun K2CompilerBridge.newRecordingCollector(
    delegate: Any,
    sink: (String, String, Boolean) -> Unit,
): Any {
    val collectorInterface = Class.forName(K2Fqns.MESSAGE_COLLECTOR, true, cl)
    return Proxy.newProxyInstance(cl, arrayOf(collectorInterface)) { _, method, args ->
        val callArgs = args ?: emptyArray()
        if (method.name == "report" && callArgs.size == REPORT_ARITY) {
            sink((callArgs[0] as Enum<*>).name, callArgs[1].toString(), callArgs[2] != null)
        }
        MethodHandles.lookup()
            .unreflect(method)
            .bindTo(delegate)
            .invokeWithArguments(callArgs.asList())
    }
}
