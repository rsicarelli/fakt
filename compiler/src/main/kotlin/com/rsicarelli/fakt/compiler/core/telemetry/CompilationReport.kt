// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import com.rsicarelli.fakt.compiler.api.LogLevel

/**
 * Generates formatted compilation reports based on log level.
 *
 * Main API for report generation. Delegates to [ReportFormatter] for actual formatting.
 *
 * **Usage:**
 * ```kotlin
 * val summary = telemetry.trackCompilation {
 *     // ... compilation logic ...
 * }
 *
 * val report = CompilationReport.generate(summary, LogLevel.INFO)
 * logger.info(report)
 * ```
 *
 * **Output Control:**
 * - QUIET: Returns empty string (no report)
 * - INFO: Concise summary (5-10 lines)
 * - DEBUG: Detailed breakdown (30-50 lines)
 * - TRACE: Exhaustive details (100+ lines)
 *
 * @see ReportFormatter
 * @see CompilationSummary
 */
object CompilationReport {
    /**
     * Generates a compilation report appropriate for the specified log level.
     *
     * @param summary The compilation summary containing all metrics
     * @param level The log level to generate the report for
     * @return Formatted report string, or empty for QUIET level
     *
     * **Example:**
     * ```kotlin
     * // INFO level - 1 line rich summary
     * val report = CompilationReport.generate(summary, LogLevel.INFO)
     * // Output: "✅ 121 fakes | 44ms | Cache: 0%"
     *
     * // DEBUG level - 5-10 lines compact
     * val debugReport = CompilationReport.generate(summary, LogLevel.DEBUG)
     * // Output: Multi-line formatted report
     *
     * // QUIET level - no output
     * val quietReport = CompilationReport.generate(summary, LogLevel.QUIET)
     * // Output: "" (empty string)
     * ```
     */
    fun generate(
        summary: CompilationSummary,
        level: LogLevel,
    ): String =
        when (level) {
            LogLevel.QUIET -> "" // No report for QUIET level
            LogLevel.INFO -> ReportFormatter.formatInfo(summary)
            LogLevel.DEBUG -> ReportFormatter.formatDebug(summary)
            LogLevel.TRACE -> ReportFormatter.formatTrace(summary)
        }

    /**
     * Generates a compilation report and splits it into lines ready for logging.
     *
     * Useful when you need to log each line separately through a logger.
     *
     * @param summary The compilation summary
     * @param level The log level
     * @return List of report lines, or empty list for QUIET level
     *
     * **Example:**
     * ```kotlin
     * CompilationReport.generateLines(summary, LogLevel.INFO).forEach { line ->
     *     logger.info(line)
     * }
     * ```
     */
    fun generateLines(
        summary: CompilationSummary,
        level: LogLevel,
    ): List<String> {
        val report = generate(summary, level)
        return if (report.isEmpty()) {
            emptyList()
        } else {
            report.lines()
        }
    }

    /**
     * Generates a success message suitable for final logging.
     *
     * Always returns a message regardless of level (except QUIET).
     *
     * @param summary The compilation summary
     * @param level The log level
     * @return Success message, or empty for QUIET level
     *
     * **Example:**
     * ```kotlin
     * val successMsg = CompilationReport.successMessage(summary, LogLevel.INFO)
     * logger.info(successMsg)  // "✅ 10 fakes generated in 1.2s"
     * ```
     */
    fun successMessage(
        summary: CompilationSummary,
        level: LogLevel,
    ): String {
        if (level == LogLevel.QUIET || !summary.isSuccessful()) {
            return ""
        }

        val totalProcessed = summary.interfacesProcessed + summary.classesProcessed
        return "✅ $totalProcessed fakes generated in ${summary.formattedTotalTime()}"
    }
}
