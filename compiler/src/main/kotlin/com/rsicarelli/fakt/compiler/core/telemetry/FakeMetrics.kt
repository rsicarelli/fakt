// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern

/**
 * Metrics for a single fake generation (interface or class).
 *
 * Captures detailed metrics about the analysis and generation of a fake implementation.
 * Used for performance profiling, identifying slow fakes, and providing insights in
 * compilation reports.
 *
 * **Time precision:** Uses nanoseconds (System.nanoTime()) for accurate measurement.
 * Smart formatting converts to appropriate units (µs/ms/s) in reports.
 *
 * **Usage:**
 * ```kotlin
 * val metrics = FakeMetrics(
 *     name = "UserService",
 *     pattern = GenericPattern.NoGenerics,
 *     memberCount = 5,  // 2 properties + 3 functions
 *     typeParamCount = 0,
 *     analysisTimeNanos = 18_000_000,  // 18ms
 *     generationTimeNanos = 45_000_000,  // 45ms
 *     generatedLOC = 87,
 *     fileSizeBytes = 2_450,
 *     importCount = 3
 * )
 * ```
 *
 * @property name Fake name (interface or class simple name, not FQN)
 * @property pattern Detected generic pattern (NoGenerics, ClassLevel, etc)
 * @property memberCount Total number of members (properties + functions)
 * @property typeParamCount Number of type parameters
 * @property analysisTimeNanos Time spent analyzing structure (nanoseconds)
 * @property generationTimeNanos Time spent generating code (nanoseconds)
 * @property generatedLOC Lines of code generated (implementation + factory + DSL)
 * @property fileSizeBytes Size of generated code in bytes
 * @property importCount Number of imports required
 */
data class FakeMetrics(
    val name: String,
    val pattern: GenericPattern,
    val memberCount: Int,
    val typeParamCount: Int,
    val analysisTimeNanos: Long,
    val generationTimeNanos: Long,
    val generatedLOC: Int,
    val fileSizeBytes: Int = 0,
    val importCount: Int,
) {
    /**
     * Total time spent on this fake (analysis + generation) in nanoseconds.
     */
    val totalTimeNanos: Long
        get() = analysisTimeNanos + generationTimeNanos

    /**
     * Indicates if this fake is slow (took longer than threshold).
     *
     * Threshold: 100ms (100_000_000 nanoseconds) total time
     *
     * @return true if fake took >100ms to process
     */
    fun isSlow(): Boolean = totalTimeNanos > 100_000_000

    /**
     * Returns a visual indicator for slow fakes.
     *
     * @return "⚠️" if slow, "" otherwise
     */
    fun slowIndicator(): String = if (isSlow()) " ⚠️" else ""

    /**
     * Formats duration as human-readable string with smart unit selection.
     *
     * Automatically chooses the most appropriate unit:
     * - 0-999µs: Shows microseconds
     * - 1-999ms: Shows milliseconds
     * - 1s+: Shows seconds with decimal
     *
     * @return Formatted duration string (e.g., "234µs", "45ms", "1.2s")
     */
    fun formattedDuration(): String =
        com.rsicarelli.fakt.compiler.api.TimeFormatter
            .format(totalTimeNanos)

    /**
     * Formats fake metrics as a one-line summary.
     *
     * Format: "FakeName (duration) - Pattern [warning]"
     *
     * Examples:
     * - "PredicateCombiner (234µs) - NoGenerics"
     * - "PairMapper (150ms) - ClassLevel ⚠️"
     *
     * @return Formatted summary string
     */
    fun formatSummary(): String {
        val patternName =
            when (pattern) {
                is GenericPattern.NoGenerics -> "NoGenerics"
                is GenericPattern.ClassLevelGenerics -> "ClassLevel"
                is GenericPattern.MethodLevelGenerics -> "MethodLevel"
                is GenericPattern.MixedGenerics -> "Mixed"
            }
        return "$name (${formattedDuration()}) - $patternName${slowIndicator()}"
    }
}
