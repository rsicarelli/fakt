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
 * t: Fakt: [jvm] FAKT COMPILATION REPORT (TRACE)
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

        val breakdown = when {
            newFakes == 0 && cachedFakes > 0 -> "($cachedFakes cached)"
            newFakes > 0 && cachedFakes == 0 -> "($newFakes new)"
            else -> "($newFakes new, $cachedFakes cached)"
        }

        return "✅ $total fakes $breakdown | ${summary.totalTimeMs}ms"
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
            appendLine(
                "Discovery: ${summary.interfacesDiscovered} interfaces, " +
                    "${summary.classesDiscovered} classes (${summary.discoveryTimeMs()}ms)",
            )

            // Generation (new vs cached breakdown)
            val newFakes = summary.newFakes()
            val cachedFakes = summary.cachedFakes()
            val generationMsg = when {
                newFakes == 0 && cachedFakes > 0 -> "$cachedFakes from cache"
                newFakes > 0 && cachedFakes == 0 -> "$newFakes new fakes"
                else -> "$newFakes new, $cachedFakes from cache"
            }
            appendLine("Generation: $generationMsg (${summary.generationTimeMs()}ms)")

            // LOC total
            val locFormatted = summary.formatNumber(summary.totalLOC)
            val locMsg = when {
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
     * Shows everything from DEBUG plus:
     * - Complete phase breakdown with sub-phases
     * - Top 10 slowest interfaces with details
     * - Average metrics (time/fake, LOC/file)
     * - Detailed cache breakdown by type
     * - File size information
     * - Output directory path
     *
     * **Example:**
     * ```
     * [jvm] ════════════════════════════════════════════════════════════
     * [jvm] FAKT COMPILATION REPORT (TRACE)
     * [jvm] ════════════════════════════════════════════════════════════
     * [jvm]
     * [jvm] SUMMARY:
     * [jvm]   Total fakes: 121
     * [jvm]   Total time: 44ms
     * [jvm]   Avg time/fake: 0ms
     * [jvm]
     * [jvm] PHASE BREAKDOWN:
     * [jvm]   [DISCOVERY] 2ms
     * [jvm]   [GENERATION] 42ms
     * ...
     * ```
     *
     * @param summary The compilation summary to format
     * @param targetName Name of the compilation target
     * @return Multi-line formatted detailed report
     */
    fun formatTrace(summary: CompilationSummary): String =
        buildString {
            val wideSeparator = "═".repeat(60)

            // Header
            appendLine(wideSeparator)
            appendLine("FAKT COMPILATION REPORT (TRACE)")
            appendLine(wideSeparator)
            appendLine()

            // Summary section
            appendLine("SUMMARY:")
            appendLine("  Total fakes: ${summary.formatNumber(summary.totalDiscovered())} (${summary.newFakes()} new, ${summary.cachedFakes()} cached)")
            appendLine("  Total time: ${summary.totalTimeMs}ms")
            val avgTime = if (summary.totalProcessed() > 0) summary.totalTimeMs / summary.totalProcessed() else 0
            appendLine("  Avg time/fake: ${avgTime}ms")
            appendLine()

            // Phase breakdown
            appendLine("PHASE BREAKDOWN:")
            summary.phaseBreakdown.entries
                .sortedBy { it.value.startTime }
                .forEach { (_, metrics) ->
                    appendLine("  [${metrics.name}] ${metrics.duration}ms")
                    metrics.subPhases.forEach { sub ->
                        appendLine("    ├─ ${sub.name} (${sub.duration}ms)")
                    }
                }
            appendLine()

            // Cache statistics
            appendLine("CACHE STATISTICS:")
            appendLine("  Hit rate: ${summary.cacheHitRate().toInt()}%")
            appendLine("  Interfaces: ${summary.interfacesCached}/${summary.interfacesDiscovered} cached")
            appendLine("  Classes: ${summary.classesCached}/${summary.classesDiscovered} cached")
            appendLine()

            // Code generation details
            appendLine("CODE GENERATION:")
            appendLine("  Total files: ${summary.formatNumber(summary.totalFiles)}")
            appendLine("  Total LOC: ${summary.formatNumber(summary.totalLOC)}")
            appendLine("  Total size: ${summary.formattedFileSize()}")
            val avgLOC = summary.avgLOCPerFile()
            appendLine("  Avg LOC/file: ${summary.formatNumber(avgLOC)}")
            appendLine()

            // Slowest fakes (top 10)
            if (summary.fakeMetrics.isNotEmpty()) {
                appendLine("SLOWEST FAKES (Top 10):")
                summary.fakeMetrics
                    .sortedByDescending { it.totalTimeMs }
                    .take(10)
                    .forEach { metric ->
                        appendLine("  ${metric.name}: ${metric.totalTimeMs}ms (${metric.generatedLOC} LOC)")
                    }
                appendLine()
            }

            // Output directory
            appendLine("OUTPUT:")
            appendLine("  ${summary.outputDirectory}")

            // Footer
            append(wideSeparator)
        }
}
