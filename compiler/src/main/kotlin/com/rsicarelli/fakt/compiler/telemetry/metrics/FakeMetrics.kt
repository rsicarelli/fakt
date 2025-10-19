// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry.metrics

import com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern

/**
 * Metrics for a single fake generation (interface or class).
 *
 * Captures detailed metrics about the analysis and generation of a fake implementation.
 * Used for performance profiling, identifying slow fakes, and providing insights in
 * compilation reports.
 *
 * **Usage:**
 * ```kotlin
 * val metrics = FakeMetrics(
 *     name = "UserService",
 *     pattern = GenericPattern.NoGenerics,
 *     memberCount = 5,  // 2 properties + 3 functions
 *     typeParamCount = 0,
 *     analysisTimeMs = 18,
 *     generationTimeMs = 45,
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
 * @property analysisTimeMs Time spent analyzing structure (ms)
 * @property generationTimeMs Time spent generating code (ms)
 * @property generatedLOC Lines of code generated (implementation + factory + DSL)
 * @property fileSizeBytes Size of generated code in bytes
 * @property importCount Number of imports required
 */
data class FakeMetrics(
    val name: String,
    val pattern: GenericPattern,
    val memberCount: Int,
    val typeParamCount: Int,
    val analysisTimeMs: Long,
    val generationTimeMs: Long,
    val generatedLOC: Int,
    val fileSizeBytes: Int = 0,
    val importCount: Int,
) {
    /**
     * Total time spent on this fake (analysis + generation).
     */
    val totalTimeMs: Long
        get() = analysisTimeMs + generationTimeMs

    /**
     * Indicates if this fake is slow (took longer than threshold).
     *
     * Threshold: 100ms total time
     *
     * @return true if fake took >100ms to process
     */
    fun isSlow(): Boolean = totalTimeMs > 100

    /**
     * Returns a visual indicator for slow fakes.
     *
     * @return "⚠️" if slow, "" otherwise
     */
    fun slowIndicator(): String = if (isSlow()) " ⚠️" else ""

    /**
     * Formats fake metrics as a one-line summary.
     *
     * Format: "FakeName (42ms) - Pattern [warning]"
     *
     * Examples:
     * - "PredicateCombiner (18ms) - NoGenerics"
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
        return "$name (${totalTimeMs}ms) - $patternName${slowIndicator()}"
    }
}
