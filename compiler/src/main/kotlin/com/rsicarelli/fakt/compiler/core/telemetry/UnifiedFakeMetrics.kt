// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

/**
 * Unified metrics combining FIR analysis and IR generation for a single fake.
 *
 * This combines timing and metadata from both the FIR validation phase and the IR generation phase,
 * providing a complete view of fake generation performance in a single structure.
 *
 * **Usage:**
 * ```kotlin
 * val metrics = UnifiedFakeMetrics(
 *     name = "UserService",
 *     firTimeNanos = 45_000,           // 45µs FIR analysis
 *     firTypeParamCount = 0,
 *     firMemberCount = 5,
 *     irTimeNanos = 1_200_000,         // 1.2ms IR generation
 *     irLOC = 73,
 * )
 *
 * // Total time = 45µs + 1.2ms = 1.245ms
 * println("Total: ${metrics.totalTimeNanos}ns")
 * ```
 *
 * **Cache Detection:**
 * Cache hits naturally show fast IR times (~5-50µs) vs fresh generation (~500µs-5ms):
 * ```kotlin
 * val cached = UnifiedFakeMetrics(
 *     name = "CachedService",
 *     firTimeNanos = 30_000,    // 30µs FIR
 *     irTimeNanos = 8_000,      // 8µs IR (cache hit!)
 *     // ... other fields
 * )
 * // totalTimeNanos = 38µs → clearly a cache hit
 * ```
 *
 * @property name The simple name of the interface or class (e.g., "UserService")
 * @property firTimeNanos Time spent in FIR analysis phase (validation, metadata extraction)
 * @property firTypeParamCount Number of type parameters found during FIR analysis
 * @property firMemberCount Number of members (properties + functions) found during FIR analysis
 * @property irTimeNanos Time spent in IR generation phase (code generation, file I/O)
 * @property irLOC Lines of code generated in the IR phase
 */
data class UnifiedFakeMetrics(
    val name: String,
    val firTimeNanos: Long,
    val firTypeParamCount: Int,
    val firMemberCount: Int,
    val irTimeNanos: Long,
    val irLOC: Int,
) {
    /**
     * Total time combining FIR analysis and IR generation.
     *
     * Fast total times (~50-200µs) indicate cache hits.
     * Slow total times (~1-10ms) indicate fresh generation.
     */
    val totalTimeNanos: Long
        get() = firTimeNanos + irTimeNanos
}
