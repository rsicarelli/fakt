// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import com.rsicarelli.fakt.compiler.api.TimeFormatter

/** Width for time value display in metrics output formatting (characters). */
private const val TIME_DISPLAY_WIDTH = 10

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
 * ├─ FIR Time                                                            234µs
 * ├─ IR Time                                                             1.2ms
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
    /** Interface count (used when metrics lists are empty at INFO level) */
    val interfaceCount: Int = interfaces.size,
    /** Class count (used when metrics lists are empty at INFO level) */
    val classCount: Int = classes.size,
    val interfaceCacheHits: Int = 0,
    val classCacheHits: Int = 0,
    val irCacheHits: Int = 0,
    val transformationTimeNanos: Long = 0,
    val savedFirTimeNanos: Long = 0,
    /** Aggregate IR time (used when metrics lists are empty at INFO level) */
    val aggregateIrTimeNanos: Long = 0,
) {
    /** Total fakes (interfaces + classes) */
    val totalFakes: Int get() = interfaceCount + classCount

    /** Total items loaded from KMP cache (skipped FIR analysis) */
    val totalFirCacheHits: Int get() = interfaceCacheHits + classCacheHits

    /** Whether all fakes were IR-cached (nothing regenerated) */
    val allIrCached: Boolean get() = irCacheHits == totalFakes && totalFakes > 0
    /**
     * FIR Time across all fakes (interfaces + classes).
     */
    val totalFirTimeNanos: Long
        get() = interfaces.sumOf { it.firTimeNanos } + classes.sumOf { it.firTimeNanos }

    /**
     * IR Time across all fakes (interfaces + classes).
     * Falls back to aggregateIrTimeNanos when metrics lists are empty (INFO level).
     */
    val totalIrTimeNanos: Long
        get() {
            val computed = interfaces.sumOf { it.irTimeNanos } + classes.sumOf { it.irTimeNanos }
            return if (computed > 0) computed else aggregateIrTimeNanos
        }

    /**
     * Total time (FIR + IR + transformation) across all fakes.
     */
    val totalTimeNanos: Long
        get() = totalFirTimeNanos + totalIrTimeNanos + transformationTimeNanos

    /** Total lines of code generated across all fakes. */
    val totalLOC: Int
        get() = interfaces.sumOf { it.irLOC } + classes.sumOf { it.irLOC }

    /** Average time per fake in nanoseconds. */
    val avgTimePerFakeNanos: Long
        get() = if (totalFakes > 0) totalTimeNanos / totalFakes else 0

    /** Average lines of code per fake. */
    val avgLOCPerFake: Int
        get() = if (totalFakes > 0) totalLOC / totalFakes else 0

    /** Cache hit rate as percentage (0-100). */
    val cacheHitRate: Int
        get() = if (totalFakes > 0) (irCacheHits * 100) / totalFakes else 0

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
            appendLine("Fakt Trace")

            // Line 1: FIR→IR cache transformation
            appendLine(
                formatLine(
                    "   ├─ FIR→IR cache transformation",
                    TimeFormatter.format(transformationTimeNanos),
                    targetColumn,
                ),
            )

            // Line 2: FIR Time with cache info
            val firLabel = when {
                totalFirCacheHits > 0 && savedFirTimeNanos > 0 ->
                    "   ├─ FIR Time ($totalFirCacheHits from cache saved ${TimeFormatter.format(savedFirTimeNanos)})"
                totalFirCacheHits > 0 ->
                    "   ├─ FIR Time ($totalFirCacheHits from cache)"
                else ->
                    "   ├─ FIR Time"
            }
            appendLine(
                formatLine(
                    firLabel,
                    TimeFormatter.format(totalFirTimeNanos),
                    targetColumn,
                ),
            )

            // Line 3: IR Time with cache info
            val irLabel = if (irCacheHits > 0) {
                "   ├─ IR Time ($irCacheHits from cache)"
            } else {
                "   ├─ IR Time"
            }
            appendLine(
                formatLine(
                    irLabel,
                    TimeFormatter.format(totalIrTimeNanos),
                    targetColumn,
                ),
            )

            // Line 4: Total time
            appendLine(
                formatLine(
                    "   ├─ Total time",
                    TimeFormatter.format(totalTimeNanos),
                    targetColumn,
                ),
            )

            // Stats section - use └─ when all cached (last section), ├─ otherwise
            val statsPrefix = if (allIrCached) "└─" else "├─"
            val statsLinePrefix = if (allIrCached) "   " else "│  "
            appendLine("   $statsPrefix Stats")
            appendLine(
                formatLine(
                    "   $statsLinePrefix├─ Total fakes",
                    totalFakes.toString(),
                    targetColumn,
                ),
            )
            appendLine(
                formatLine(
                    "   $statsLinePrefix├─ Avg Time per Fake",
                    TimeFormatter.format(avgTimePerFakeNanos),
                    targetColumn,
                ),
            )
            // Combine interfaces and classes metrics (only regenerated/generated fakes)
            val allMetrics = interfaces + classes

            // Cache hit rate - use └─ if all cached or no metrics, ├─ otherwise
            val cacheHitCloser = if (allIrCached || allMetrics.isEmpty()) "└─" else "├─"
            appendLine(
                formatLine(
                    "   $statsLinePrefix$cacheHitCloser Cache hit rate",
                    "$cacheHitRate%",
                    targetColumn,
                ),
            )

            // When all cached, close the tree here
            if (allIrCached) {
                return@buildString
            }

            // Show generated fakes (only when there are metrics)
            // This includes both new fakes and regenerated ones (signature changed)
            if (allMetrics.isNotEmpty()) {
                appendLine("   └─ Generated: ${allMetrics.size}")

                allMetrics.forEachIndexed { index, metric ->
                    val isLast = index == allMetrics.size - 1
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
        // Determine prefixes based on position (3 spaces for "i: " alignment)
        val prefix =
            when {
                isTopLevel && isLast -> "      └─"
                isTopLevel -> "      ├─"
                isLast -> "   │  └─"
                else -> "   │  ├─"
            }

        val detailPrefix =
            when {
                isTopLevel && isLast -> "         "
                isTopLevel -> "      │  "
                isLast -> "         "
                else -> "   │  │  "
            }

        // Format time value
        val totalTime = TimeFormatter.format(metric.totalTimeNanos)

        // Line 1: Fake name with total time
        appendLine(formatLine("$prefix ${metric.name}", totalTime, targetColumn))

        // Line 2: Generated fake class with LOC
        val fakeImplLine = "$detailPrefix└─ Fake${metric.name}Impl"
        appendLine(formatLine(fakeImplLine, "${metric.irLOC} LOC", targetColumn))
    }

    /**
     * Formats metrics as a concise INFO-level summary (4 lines).
     *
     * Provides essential compilation metrics without detailed per-fake breakdown.
     * Uses explicit IR cache hit tracking for accurate cache reporting.
     *
     * **Output Scenarios:**
     *
     * All cached (happy path):
     * ```
     * Fakt: 101 fakes (all cached)
     * ```
     *
     * Some regenerated:
     * ```
     * Fakt: 101 fakes in 1.2ms (3 regenerated, 98 cached)
     * ```
     *
     * Nothing cached (first build):
     * ```
     * Fakt: 101 fakes generated in 234ms
     * ```
     *
     * This format is designed for normal development builds where developers
     * want confirmation that fakes were generated without detailed metrics.
     * Uses explicit IR cache hit tracking instead of timing heuristic.
     *
     * @return Concise INFO summary string ready for logging
     */
    fun toInfoSummary(): String {
        // All cached - minimal output
        if (allIrCached) {
            return "Fakt: $totalFakes fakes (all cached)"
        }

        val totalTime = TimeFormatter.format(totalTimeNanos)
        val regenerated = totalFakes - irCacheHits

        // Some cached - show breakdown
        return if (irCacheHits > 0) {
            "Fakt: $totalFakes fakes in $totalTime ($regenerated regenerated, $irCacheHits cached)"
        } else {
            // Nothing cached - normal generation message
            "Fakt: $totalFakes fakes generated in $totalTime"
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
     * formatLine("├─ FIR Time:", "234µs", 80)
     * // → "├─ FIR Time:                                                         234µs"
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

    /**
     * Formats a number with thousand separators (e.g., 4521 → "4,521").
     */
    private fun formatNumber(number: Int): String =
        "%,d".format(number)
}
