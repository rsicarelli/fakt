// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

import com.rsicarelli.fakt.compiler.telemetry.metrics.CompilationSummary

/**
 * Formats compilation reports for different log levels.
 *
 * Provides clean, professional output with appropriate detail for each level:
 * - **QUIET**: No output (handled elsewhere)
 * - **INFO**: 1 line rich summary with target name
 * - **DEBUG**: 5-10 lines compact with text-only style
 * - **TRACE**: 30+ lines exhaustive with all details
 *
 * **Output Examples:**
 *
 * **INFO** (1 line):
 * ```
 * i: Fakt: [mingwX64] ✅ 121 fakes | 44ms | Cache: 0%
 * ```
 *
 * **DEBUG** (5-10 lines):
 * ```
 * i: Fakt: [jvm] Discovery: 100 interfaces, 21 classes (2ms)
 * i: Fakt: [jvm] Generation: 121 fakes (42ms)
 * i: Fakt: [jvm] ════════════════════════════════════════
 * i: Fakt: [jvm] Summary: 121 fakes in 44ms
 * i: Fakt: [jvm] Cache: 0% (0/121 cached)
 * i: Fakt: [jvm] Output: 1,234 LOC across 121 files
 * i: Fakt: [jvm] ════════════════════════════════════════
 * ```
 *
 * **TRACE** (30+ lines):
 * ```
 * t: Fakt: [jvm] ════════════════════════════════════════════════════════════
 * t: Fakt: [jvm] COMPILATION REPORT (TRACE)
 * ... (all details)
 * ```
 *
 * @see CompilationSummary
 */
object ReportFormatter {
    private const val SEPARATOR = "════════════════════════════════════════"

    /**
     * Formats a compilation summary for INFO level (1 line, rich).
     *
     * Format: `✅ N fakes (X new, Y cached) | XXms`
     *
     * Shows:
     * - Success indicator (✅)
     * - Total fakes with breakdown of new vs cached
     * - Total time
     *
     * **Example:**
     * ```
     * ✅ 121 fakes (121 new) | 44ms
     * ✅ 121 fakes (121 cached) | 1ms
     * ✅ 121 fakes (5 new, 116 cached) | 25ms
     * ```
     *
     * @param summary The compilation summary to format
     * @return Single-line formatted summary
     */
    fun formatInfo(summary: CompilationSummary): String {
        val total = summary.totalDiscovered()
        val newFakes = summary.newFakes()
        val cachedFakes = summary.cachedFakes()

        val breakdown =
            when {
                newFakes == 0 && cachedFakes > 0 -> "($cachedFakes cached)"
                newFakes > 0 && cachedFakes == 0 -> "($newFakes new)"
                else -> "($newFakes new, $cachedFakes cached)"
            }

        return "✅ $total fakes $breakdown | ${summary.formattedTotalTime()}"
    }

    /**
     * Formats a compilation summary for DEBUG level (3-4 lines, clean).
     *
     * Shows:
     * - Discovery (interfaces + classes count + timing)
     * - Generation (new vs cached breakdown + timing)
     * - Total LOC (with indication if all cached)
     *
     * **Example:**
     * ```
     * Discovery: 100 interfaces, 21 classes (2ms)
     * Generation: 121 new fakes (53ms)
     * 3,867 LOC total
     * ```
     *
     * **Example (all cached):**
     * ```
     * Discovery: 100 interfaces, 21 classes (1ms)
     * Generation: 121 from cache (0ms)
     * 3,867 LOC total (all cached)
     * ```
     *
     * **Example (mix):**
     * ```
     * Discovery: 100 interfaces, 21 classes (2ms)
     * Generation: 5 new, 116 from cache (12ms)
     * 3,867 LOC total (248 new)
     * ```
     *
     * @param summary The compilation summary to format
     * @return Multi-line formatted report
     */
    fun formatDebug(summary: CompilationSummary): String =
        buildString {
            // Discovery
            val discoveryTime = summary.getPhase("DISCOVERY")?.formattedDuration() ?: "0µs"
            appendLine(
                "Discovery: ${summary.interfacesDiscovered} interfaces, " +
                    "${summary.classesDiscovered} classes ($discoveryTime)",
            )

            // Generation (new vs cached breakdown)
            val newFakes = summary.newFakes()
            val cachedFakes = summary.cachedFakes()
            val generationMsg =
                when {
                    newFakes == 0 && cachedFakes > 0 -> "$cachedFakes from cache"
                    newFakes > 0 && cachedFakes == 0 -> "$newFakes new fakes"
                    else -> "$newFakes new, $cachedFakes from cache"
                }
            val generationTime = summary.getPhase("GENERATION")?.formattedDuration() ?: "0µs"
            appendLine("Generation: $generationMsg ($generationTime)")

            // LOC total
            val locFormatted = summary.formatNumber(summary.totalLOC)
            val locMsg =
                when {
                    summary.totalFiles == 0 -> "All from cache (0 new LOC)"
                    cachedFakes == 0 -> "$locFormatted LOC total"
                    newFakes == 0 -> "$locFormatted LOC total (all cached)"
                    else -> {
                        // Calculate new LOC (approximate: total * (new / total discovered))
                        val newLOCApprox = (summary.totalLOC * newFakes) / (newFakes + cachedFakes)
                        "$locFormatted LOC total (${summary.formatNumber(newLOCApprox)} new)"
                    }
                }
            appendLine(locMsg)
        }

    /**
     * Formats a compilation summary for TRACE level (30+ lines, exhaustive).
     *
     * Shows everything with tree-style hierarchy (├─ └─):
     * - Discovery phase with all interface/class names
     * - Generation metrics with LOC breakdown
     * - Cache statistics (count only)
     * - Top 10 slowest fakes with timings
     * - Output directory
     *
     * **Example:**
     * ```
     * DISCOVERY (234µs)
     * ├─ Interfaces (100)
     * │  ContravariantListConsumer, CovariantListProducer, ...
     * └─ Classes (21)
     *    AsyncDataFetcher, PaymentProcessor, ...
     *
     * GENERATION (30ms)
     * ├─ New fakes: 121
     * ├─ From cache: 0
     * └─ Total LOC: 3,867 (avg 32 LOC/file)
     * ```
     *
     * @param summary The compilation summary to format
     * @return Multi-line formatted detailed report with tree structure
     */
    fun formatTrace(summary: CompilationSummary): String =
        buildString {
            val wideSeparator = "═".repeat(60)

            // Header separator
            appendLine(wideSeparator)

            // Discovery phase (tree-style)
            val discoveryTime = summary.getPhase("DISCOVERY")?.formattedDuration() ?: "0µs"
            appendLine("DISCOVERY ($discoveryTime)")
            appendLine("├─ Interfaces: ${summary.interfacesDiscovered}")
            appendLine("└─ Classes: ${summary.classesDiscovered}")

            // Generation phase (tree-style)
            val generationTime = summary.getPhase("GENERATION")?.formattedDuration() ?: "0µs"
            appendLine("GENERATION ($generationTime)")
            appendLine("├─ New fakes: ${summary.newFakes()}")
            appendLine("├─ From cache: ${summary.cachedFakes()}")
            val avgLOC = summary.avgLOCPerFile()
            appendLine("└─ Total LOC: ${summary.formatNumber(summary.totalLOC)} (avg $avgLOC LOC/file)")

            // Summary section (tree-style)
            appendLine("SUMMARY (${summary.formattedTotalTime()})")
            appendLine(
                "├─ Total fakes: ${summary.formatNumber(
                    summary.totalDiscovered(),
                )} (${summary.newFakes()} new, ${summary.cachedFakes()} cached)",
            )
            val avgTimeNanos = if (summary.totalProcessed() > 0) summary.totalTimeNanos / summary.totalProcessed() else 0
            appendLine("└─ Avg time/fake: ${TimeFormatter.format(avgTimeNanos)}")

            // Slowest fakes (tree-style, top 10)
            if (summary.fakeMetrics.isNotEmpty()) {
                appendLine("SLOWEST FAKES (Top 10):")
                summary.fakeMetrics
                    .sortedByDescending { it.totalTimeNanos }
                    .take(10)
                    .forEachIndexed { index, metric ->
                        val prefix = if (index == 9 || index == summary.fakeMetrics.size - 1) "└─" else "├─"
                        appendLine(
                            "$prefix ${index + 1}. ${metric.name} (${metric.formattedDuration()}) - ${metric.generatedLOC} LOC${metric.slowIndicator()}",
                        )
                    }
                appendLine()
            }

            // Footer
            append(wideSeparator)
        }

    /**
     * Wraps a list of names into multiple lines with proper indentation.
     *
     * @param names List of names to wrap
     * @param indent Indentation prefix for each line
     * @param maxWidth Maximum line width
     * @return Formatted multi-line string with wrapped names
     */
    private fun wrapNames(
        names: List<String>,
        indent: String,
        maxWidth: Int,
    ): String =
        buildString {
            val truncatedNames = if (names.size > 20) names.take(20) + "..." else names
            var currentLine = indent
            truncatedNames.forEachIndexed { index, name ->
                val separator = if (index < truncatedNames.size - 1) ", " else ""
                val token = name + separator

                if (currentLine.length + token.length > maxWidth) {
                    appendLine(currentLine)
                    currentLine = indent + token
                } else {
                    currentLine += token
                }
            }
            if (currentLine.isNotEmpty()) {
                appendLine(currentLine)
            }
        }
}
