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

    private val heldBack = mutableListOf<() -> Unit>()

    /**
     * Sink for `K2CompilerBridge.newRecordingCollector`. Returns true when the message is a
     * tolerated error that the caller must NOT print now: [replay] is kept so [replayHeldBack] can
     * print it later if the run fails.
     */
    fun record(
        severity: String,
        message: String,
        hasLocation: Boolean,
        replay: () -> Unit,
    ): Boolean {
        val verdict = classify(severity, message, hasLocation)
        if (verdict != DiagnosticVerdict.IGNORED) {
            synchronized(recorded) { recorded += RecordedDiagnostic(message, verdict) }
        }
        val hold = verdict == DiagnosticVerdict.TOLERATED
        if (hold) synchronized(heldBack) { heldBack += replay }
        return hold
    }

    /** True once any tolerated error was held back; keeps the collector's `hasErrors()` honest. */
    fun hasHeldBack(): Boolean = synchronized(heldBack) { heldBack.isNotEmpty() }

    /** Prints every held-back error through the original collector; call it when the run fails. */
    fun replayHeldBack() {
        val pending = synchronized(heldBack) { heldBack.toList().also { heldBack.clear() } }
        pending.forEach { it() }
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
 * A `MessageCollector` proxy over [delegate] (the printing collector). Everything is forwarded
 * unchanged except tolerated errors: [recorder] decides to hold those back (see
 * [AnalysisRecorder.record]) so a green build does not print scary `error:` lines. `hasErrors()`
 * still answers true for held-back errors, so K2's control flow and exit code do not change. No
 * compiler types appear in the signature.
 */
internal fun K2CompilerBridge.newRecordingCollector(
    delegate: Any,
    recorder: AnalysisRecorder,
): Any {
    val collectorInterface = Class.forName(K2Fqns.MESSAGE_COLLECTOR, true, cl)
    return Proxy.newProxyInstance(cl, arrayOf(collectorInterface)) { _, method, args ->
        val callArgs = args ?: emptyArray()
        val forward = {
            MethodHandles.lookup()
                .unreflect(method)
                .bindTo(delegate)
                .invokeWithArguments(callArgs.asList())
        }
        when {
            method.name == "report" && callArgs.size == REPORT_ARITY -> {
                val severity = (callArgs[0] as Enum<*>).name
                val held =
                    recorder.record(severity, callArgs[1].toString(), callArgs[2] != null) {
                        forward()
                    }
                if (held) null else forward()
            }
            method.name == "hasErrors" && callArgs.isEmpty() ->
                forward() as Boolean || recorder.hasHeldBack()
            else -> forward()
        }
    }
}
