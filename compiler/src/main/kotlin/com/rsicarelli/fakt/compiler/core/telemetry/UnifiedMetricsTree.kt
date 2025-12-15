// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import com.rsicarelli.fakt.compiler.api.TimeFormatter

/** Width for time value display in metrics output formatting (characters). */
private const val TIME_DISPLAY_WIDTH = 10

/**
 * Threshold in nanoseconds for cache hit detection (100µs).
 *
 * IR generation times below this threshold indicate cached fakes.
 * Fresh generation: 500µs-5ms, Cached: ~5-50µs.
 */
private const val CACHE_HIT_THRESHOLD_NANOS = 100_000L

/** Multiplier for converting decimal to percentage (e.g., 0.75 * 100 = 75%). */
private const val PERCENTAGE_MULTIPLIER = 100

/**
 * Aggregate metrics combining all FIR and IR data for tree-style logging.
 *
 * Encapsulates the complete metrics hierarchy (interfaces + classes) and provides
 * tree-formatted output suitable for DEBUG/TRACE logging.
 *
 * **Purpose:**
 * - Consolidates metrics from multiple fakes into a single tree structure
 * - Eliminates logging filtering issues by building complete output as single string
 * - Provides clean separation between data collection and presentation logic
 *
 * **Usage:**
 * ```kotlin
 * val tree = UnifiedMetricsTree(
 *     interfaces = interfaceMetrics,
 *     classes = classMetrics
 * )
 *
 * logger.debug(tree.toTreeString())
 * ```
 *
 * **Output Format:**
 * ```
 * FIR + IR trace
 * ├─ Total FIR time                                                            234µs
 * ├─ Total IR time                                                             1.2ms
 * ├─ Total time                                                                1.4ms
 * ├─ Interfaces: 3
 * │  ├─ UserService                                                            580µs
 * │  │  ├─ FIR analysis: 0 type parameters, 5 members                          45µs
 * │  │  └─ IR generation: FakeUserServiceImpl 73 LOC                           535µs
 * │  ├─ DataCache                                                              340µs
 * │  │  ├─ FIR analysis: 1 type parameters, 3 members                          40µs
 * │  │  └─ IR generation: FakeDataCacheImpl 58 LOC                             300µs
 * └─ Classes: 1
 *    └─ DataHolder                                                             120µs
 *       ├─ FIR analysis: 1 type parameters, 2 members                          30µs
 *       └─ IR generation: FakeDataHolderImpl 45 LOC                            90µs
 * ```
 *
 * @property interfaces List of metrics for @Fake annotated interfaces
 * @property classes List of metrics for @Fake annotated classes
 */
data class UnifiedMetricsTree(
    val interfaces: List<UnifiedFakeMetrics>,
    val classes: List<UnifiedFakeMetrics>,
) {
    /**
     * Total FIR time across all fakes (interfaces + classes).
     */
    val totalFirTimeNanos: Long
        get() = interfaces.sumOf { it.firTimeNanos } + classes.sumOf { it.firTimeNanos }

    /**
     * Total IR time across all fakes (interfaces + classes).
     */
    val totalIrTimeNanos: Long
        get() = interfaces.sumOf { it.irTimeNanos } + classes.sumOf { it.irTimeNanos }

    /**
     * Total time (FIR + IR) across all fakes.
     */
    val totalTimeNanos: Long
        get() = totalFirTimeNanos + totalIrTimeNanos

    /**
     * Formats metrics as a tree-structured string with proper branching.
     *
     * Builds the entire tree as a single string to avoid logging system filtering issues
     * (e.g., Gradle's consecutive duplicate suppression).
     *
     * The tree uses Unicode box-drawing characters for visual structure:
     * - `├─` branch node (more items follow)
     * - `└─` final node (last item in section)
     * - `│` vertical continuation line
     *
     * Time values are right-aligned at the target column for clean visual alignment.
     *
     * @param targetColumn Column for right-aligning time values (default: 80)
     * @return Multi-line tree-formatted string ready for logging
     */
    fun toTreeString(targetColumn: Int = 80): String =
        buildString {
            appendLine("FIR + IR trace")
            appendLine(
                formatLine(
                    "├─ Total FIR time",
                    TimeFormatter.format(totalFirTimeNanos),
                    targetColumn,
                ),
            )
            appendLine(
                formatLine(
                    "├─ Total IR time",
                    TimeFormatter.format(totalIrTimeNanos),
                    targetColumn,
                ),
            )
            appendLine(
                formatLine(
                    "├─ Total time",
                    TimeFormatter.format(totalTimeNanos),
                    targetColumn,
                ),
            )
            appendLine("├─ Interfaces: ${interfaces.size}")

            interfaces.forEachIndexed { index, metric ->
                // If there are classes coming after, no interface should close the branch
                val isLast = index == interfaces.size - 1 && classes.isEmpty()
                appendMetricTree(metric, isLast, isTopLevel = false, targetColumn)
            }

            if (classes.isNotEmpty()) {
                appendLine("└─ Classes: ${classes.size}")
                classes.forEachIndexed { index, metric ->
                    val isLast = index == classes.size - 1
                    appendMetricTree(metric, isLast, isTopLevel = true, targetColumn)
                }
            }
        }.trimEnd() // Remove trailing newline for cleaner output

    /**
     * Appends a single metric's tree structure (3 lines: header + FIR + IR).
     *
     * @param metric The fake metrics to format
     * @param isLast Whether this is the last item in its section
     * @param isTopLevel Whether this is a top-level section (classes use different prefix)
     * @param targetColumn Column for right-aligning time values
     */
    private fun StringBuilder.appendMetricTree(
        metric: UnifiedFakeMetrics,
        isLast: Boolean,
        isTopLevel: Boolean,
        targetColumn: Int,
    ) {
        // Determine prefixes based on position
        val prefix =
            when {
                isTopLevel && isLast -> "   └─"
                isTopLevel -> "   ├─"
                isLast -> "│  └─"
                else -> "│  ├─"
            }

        val detailPrefix =
            when {
                isTopLevel && isLast -> "      "
                isTopLevel -> "   │  "
                isLast -> "      "
                else -> "│  │  "
            }

        // Format time values
        val totalTime = TimeFormatter.format(metric.totalTimeNanos)
        val firTime = TimeFormatter.format(metric.firTimeNanos)
        val irTime = TimeFormatter.format(metric.irTimeNanos)

        // Line 1: Fake name with total time
        appendLine(formatLine("$prefix ${metric.name}", totalTime, targetColumn))

        // Line 2: FIR analysis details
        val firLine =
            "$detailPrefix├─ FIR analysis: ${metric.firTypeParamCount} type parameters, " +
                "${metric.firMemberCount} members"
        appendLine(formatLine(firLine, firTime, targetColumn))

        // Line 3: IR generation details
        val irLine = "$detailPrefix└─ IR generation: Fake${metric.name}Impl ${metric.irLOC} LOC"
        appendLine(formatLine(irLine, irTime, targetColumn))
    }

    /**
     * Formats metrics as a concise INFO-level summary (4 lines).
     *
     * Provides essential compilation metrics without detailed per-fake breakdown:
     * - Total fakes generated (interfaces + classes)
     * - Total compilation time (FIR + IR)
     * - Phase breakdown (FIR vs IR time)
     * - Cache statistics (estimated based on IR timing heuristic)
     *
     * **Output Format:**
     * ```
     * Fakt: 3 fakes generated in 1.4ms (0 cached)
     *   Interfaces: 3 | Classes: 0
     *   FIR: 115µs | IR: 1.285ms
     *   Cache: 0/3 (0%)
     * ```
     *
     * This format is designed for normal development builds where developers
     * want confirmation that fakes were generated without detailed metrics.
     *
     * **Cache Detection Heuristic:**
     * IR time < 100µs per fake indicates cache hit (fresh generation typically 500µs-5ms).
     *
     * @return Multi-line INFO summary string ready for logging
     */
    fun toInfoSummary(): String {
        val totalFakes = interfaces.size + classes.size
        val totalTime = TimeFormatter.format(totalTimeNanos)
        val firTime = TimeFormatter.format(totalFirTimeNanos)
        val irTime = TimeFormatter.format(totalIrTimeNanos)

        // Cache detection: IR time < 100µs per fake indicates cache hit
        // (Fresh generation typically 500µs-5ms, cached ~5-50µs)
        val avgIrTimePerFake = if (totalFakes > 0) totalIrTimeNanos / totalFakes else 0
        val estimatedCached = if (avgIrTimePerFake < CACHE_HIT_THRESHOLD_NANOS) totalFakes else 0

        val cachePercent =
            if (totalFakes > 0) {
                (estimatedCached * PERCENTAGE_MULTIPLIER) / totalFakes
            } else {
                0
            }

        return buildString {
            appendLine(
                "Fakt: $totalFakes fakes generated in $totalTime ($estimatedCached cached)",
            )
            appendLine("  Interfaces: ${interfaces.size} | Classes: ${classes.size}")
            appendLine("  FIR: $firTime | IR: $irTime")
            append("  Cache: $estimatedCached/$totalFakes ($cachePercent%)")
        }
    }

    /**
     * Formats a line with right-aligned time value at the target column.
     *
     * The time value is padded to 10 characters and right-aligned at the target column.
     * If the text is too long, it will overflow and the time will be appended with a single space.
     *
     * **Examples:**
     * ```
     * formatLine("├─ Total FIR time:", "234µs", 80)
     * // → "├─ Total FIR time:                                                         234µs"
     *
     * formatLine("│  ├─ UserService", "580µs total", 80)
     * // → "│  ├─ UserService                                                    580µs total"
     * ```
     *
     * @param text The main text content (left-aligned)
     * @param time The time string to right-align
     * @param targetColumn Column position for right-aligning the time value
     * @return Formatted line with right-aligned time
     */
    private fun formatLine(
        text: String,
        time: String,
        targetColumn: Int,
    ): String {
        val timeWithPadding = time.padStart(TIME_DISPLAY_WIDTH)
        val availableSpace = targetColumn - timeWithPadding.length
        return if (text.length >= availableSpace) {
            // Text is too long, just append time with single space
            "$text $time"
        } else {
            // Pad text to reach target column
            text.padEnd(availableSpace) + timeWithPadding
        }
    }
}
