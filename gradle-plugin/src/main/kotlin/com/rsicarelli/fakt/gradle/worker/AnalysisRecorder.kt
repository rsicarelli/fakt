// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.compiler.api.LogLevel
import java.lang.invoke.MethodHandles
import java.lang.reflect.Proxy

/** One message captured from the analysis run, already classified. */
internal data class RecordedDiagnostic(
    val message: String,
    /** `path:line:col`, or null when the message had no source location. Console only. */
    val location: String?,
    val verdict: DiagnosticVerdict,
)

/** Collects the diagnostics of one K2 run and turns them into the worker's decision. */
internal class AnalysisRecorder {
    private val recorded = mutableListOf<RecordedDiagnostic>()

    private val heldBack = mutableListOf<() -> Unit>()

    /**
     * Sink for `K2CompilerBridge.newRecordingCollector`. Returns true when the message is a
     * tolerated error that the caller must NOT print now: [replay] is kept so [replayHeldBack] can
     * print it later if the run fails.
     */
    fun record(severity: String, message: String, location: String?, replay: () -> Unit): Boolean {
        val verdict = classify(severity, message, hasLocation = location != null)
        if (verdict != DiagnosticVerdict.IGNORED) {
            synchronized(recorded) { recorded += RecordedDiagnostic(message, location, verdict) }
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

    fun tolerated(): List<RecordedDiagnostic> = withVerdict(DiagnosticVerdict.TOLERATED)

    fun fatal(): List<String> = withVerdict(DiagnosticVerdict.FATAL).map { it.message }

    fun decide(exitName: String): AnalysisOutcome =
        outcome(exitName, fatal().size, tolerated().size)

    private fun withVerdict(verdict: DiagnosticVerdict): List<RecordedDiagnostic> =
        synchronized(recorded) { recorded.filter { it.verdict == verdict } }
}

/**
 * Lines to log when a run succeeded despite tolerated errors. Below INFO nothing; at INFO one
 * summary; at DEBUG the summary plus every error with its location. Every line contains the stable
 * text `Fakt: tolerated compiler error` that the CI guards grep for.
 */
internal fun toleratedLogLines(
    tolerated: List<RecordedDiagnostic>,
    logLevel: LogLevel,
): List<String> {
    if (tolerated.isEmpty() || logLevel < LogLevel.INFO) return emptyList()
    val summary =
        "Fakt: tolerated compiler error(s) outside @Fake code: ${tolerated.size} " +
            "(set fakt logLevel to DEBUG to list each one)"
    val each =
        if (logLevel >= LogLevel.DEBUG) {
            tolerated.map { "Fakt: tolerated compiler error: ${it.location}: ${it.message}" }
        } else {
            emptyList()
        }
    return listOf(summary) + each
}

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
                    recorder.record(severity, callArgs[1].toString(), locationText(callArgs[2])) {
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

/**
 * `path:line:col` read reflectively from a `CompilerMessageSourceLocation` (so no compiler types
 * are needed). Null for a missing location; a placeholder when reflection fails.
 */
private fun locationText(location: Any?): String? {
    if (location == null) return null
    return runCatching {
            val type = location.javaClass
            val path = type.getMethod("getPath").invoke(location)
            val line = type.getMethod("getLine").invoke(location)
            val column = type.getMethod("getColumn").invoke(location)
            "$path:$line:$column"
        }
        .getOrElse { UNKNOWN_LOCATION }
}

private const val UNKNOWN_LOCATION = "<unknown location>"
