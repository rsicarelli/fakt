// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry.metrics

/**
 * Metrics for a single compilation phase.
 *
 * Tracks timing and hierarchical structure of compilation phases.
 * Supports nested sub-phases for detailed breakdowns.
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
 *     startTime = System.currentTimeMillis(),
 *     endTime = System.currentTimeMillis() + 340,
 *     subPhases = listOf(
 *         PhaseMetrics("PredicateCombiner", start, start + 18),
 *         PhaseMetrics("PairMapper", start + 18, start + 60)
 *     )
 * )
 * // phase.duration == 340ms
 * ```
 *
 * @property name Human-readable phase name (e.g., "DISCOVERY", "ANALYSIS")
 * @property startTime Start time in milliseconds (System.currentTimeMillis())
 * @property endTime End time in milliseconds
 * @property subPhases Optional nested sub-phases for hierarchical tracking
 */
data class PhaseMetrics(
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val subPhases: List<PhaseMetrics> = emptyList(),
) {
    /**
     * Duration of this phase in milliseconds.
     *
     * Calculated as endTime - startTime.
     */
    val duration: Long
        get() = endTime - startTime

    /**
     * Formats duration as human-readable string.
     *
     * Examples:
     * - 0-999ms: "340ms"
     * - 1000-59999ms: "1.2s"
     * - 60000+ms: "1m 5s"
     *
     * @return Formatted duration string
     */
    fun formattedDuration(): String =
        when {
            duration < 1000 -> "${duration}ms"
            duration < 60000 -> String.format("%.1fs", duration / 1000.0)
            else -> {
                val minutes = duration / 60000
                val seconds = (duration % 60000) / 1000
                "${minutes}m ${seconds}s"
            }
        }
}
