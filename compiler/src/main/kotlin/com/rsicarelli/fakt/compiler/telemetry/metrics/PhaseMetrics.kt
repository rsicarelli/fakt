// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry.metrics

/**
 * Metrics for a single compilation phase.
 *
 * Tracks timing and hierarchical structure of compilation phases.
 * Supports nested sub-phases for detailed breakdowns.
 *
 * **Time precision:** Uses nanoseconds (System.nanoTime()) for accurate measurement
 * of fast compiler operations. Smart formatting converts to appropriate units (µs/ms/s).
 *
 * **Phases in Fakt compilation:**
 * - REGISTRATION: Plugin initialization and setup
 * - DISCOVERY: Finding @Fake annotated interfaces/classes
 * - FILTERING: Generic pattern analysis and cache checking
 * - ANALYSIS: Interface structure extraction
 * - GENERATION: Code generation (implementation, factory, DSL)
 * - IMPORTS: Import resolution
 * - SOURCE_SET: Source set mapping
 * - I/O: File writing
 * - CACHE: Cache update
 *
 * **Usage:**
 * ```kotlin
 * val phase = PhaseMetrics(
 *     name = "ANALYSIS",
 *     startTime = System.nanoTime(),
 *     endTime = System.nanoTime() + 340_000_000, // +340ms in nanoseconds
 *     subPhases = listOf(
 *         PhaseMetrics("PredicateCombiner", start, start + 18_000_000),
 *         PhaseMetrics("PairMapper", start + 18_000_000, start + 60_000_000)
 *     )
 * )
 * // phase.formattedDuration() == "340ms"
 * ```
 *
 * @property name Human-readable phase name (e.g., "DISCOVERY", "ANALYSIS")
 * @property startTime Start time in nanoseconds (System.nanoTime())
 * @property endTime End time in nanoseconds
 * @property subPhases Optional nested sub-phases for hierarchical tracking
 */
data class PhaseMetrics(
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val subPhases: List<PhaseMetrics> = emptyList(),
) {
    /**
     * Duration of this phase in nanoseconds.
     *
     * Calculated as endTime - startTime.
     */
    val duration: Long
        get() = endTime - startTime

    /**
     * Formats duration as human-readable string with smart unit selection.
     *
     * Automatically chooses the most appropriate unit based on duration:
     * - 0-999µs: Shows microseconds (e.g., "234µs", "12µs")
     * - 1-999ms: Shows milliseconds (e.g., "45ms", "150ms")
     * - 1-59s: Shows seconds with decimal (e.g., "1.2s", "5.8s")
     * - 60s+: Shows minutes and seconds (e.g., "1m 5s")
     *
     * @return Formatted duration string
     */
    fun formattedDuration(): String = com.rsicarelli.fakt.compiler.telemetry.TimeFormatter.format(duration)
}
