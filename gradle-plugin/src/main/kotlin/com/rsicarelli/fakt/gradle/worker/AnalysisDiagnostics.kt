// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

/** How the worker treats one message reported by the analysis compiler run. */
internal enum class DiagnosticVerdict {
    /** Not an error (warning, info, logging): never affects the outcome. */
    IGNORED,

    /** An error in code Fakt does not need: the run may still succeed. */
    TOLERATED,

    /** An error Fakt cannot accept: the run fails. */
    FATAL,
}

/** Final decision for an analysis run. */
internal enum class AnalysisOutcome {
    SUCCESS,
    FAILURE,
}

private const val EXIT_OK = "OK"
private const val EXIT_COMPILATION_ERROR = "COMPILATION_ERROR"
private const val FAKT_ERROR_PREFIX = "[FAKT]"
private val ERROR_SEVERITIES = setOf("ERROR", "EXCEPTION")

/**
 * Classifies one reported message. Only errors matter: an error is tolerated when it points at a
 * source location and was not raised by Fakt itself. Errors without a location (arguments, plugin
 * loading, configuration), `[FAKT]` errors and exceptions are fatal.
 */
internal fun classify(severity: String, message: String, hasLocation: Boolean): DiagnosticVerdict =
    when {
        severity !in ERROR_SEVERITIES -> DiagnosticVerdict.IGNORED
        severity == "EXCEPTION" -> DiagnosticVerdict.FATAL
        !hasLocation -> DiagnosticVerdict.FATAL
        message.trimStart().startsWith(FAKT_ERROR_PREFIX) -> DiagnosticVerdict.FATAL
        else -> DiagnosticVerdict.TOLERATED
    }

/**
 * Decides the run: exit OK succeeds. COMPILATION_ERROR succeeds only when we saw at least one
 * tolerated error and no fatal one: with nothing reported through our collector, K2 failed for a
 * reason we cannot see, so the run fails closed.
 */
internal fun outcome(exitName: String, fatalCount: Int, toleratedCount: Int): AnalysisOutcome =
    when {
        exitName == EXIT_OK -> AnalysisOutcome.SUCCESS
        exitName == EXIT_COMPILATION_ERROR && fatalCount == 0 && toleratedCount > 0 ->
            AnalysisOutcome.SUCCESS
        else -> AnalysisOutcome.FAILURE
    }
