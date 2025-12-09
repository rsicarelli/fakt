// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

/**
 * Metrics collected during IR generation for a single fake interface or class.
 *
 * Used for consolidated tree-style logging that matches FIR phase format.
 * Collects timing and structural information for batch output instead of
 * immediate per-fake logging.
 *
 * **Usage Pattern:**
 * ```kotlin
 * val metrics = mutableListOf<GeneratedFakeMetrics>()
 *
 * for (metadata in interfaceMetadata) {
 *     val (analysis, analysisTime) = measureTimeNanos { ... }
 *     val (code, generationTime) = measureTimeNanos { ... }
 *
 *     metrics.add(GeneratedFakeMetrics(
 *         name = interfaceName,
 *         typeParamCount = analysis.typeParameters.size,
 *         memberCount = analysis.properties.size + analysis.functions.size,
 *         generatedLOC = code.calculateTotalLOC(),
 *         analysisTimeNanos = analysisTime,
 *         generationTimeNanos = generationTime
 *     ))
 * }
 *
 * // Later: batch log all metrics at once
 * IrGenerationLogging.logIrGenerationTrace(logger, metrics)
 * ```
 *
 * @property name Simple interface or class name (e.g., "UserService")
 * @property typeParamCount Number of type parameters (class-level)
 * @property memberCount Total members (properties + functions/methods)
 * @property generatedLOC Lines of code generated for fake implementation
 * @property analysisTimeNanos Time spent on IR analysis/transformation
 * @property generationTimeNanos Time spent on code generation
 */
data class GeneratedFakeMetrics(
    val name: String,
    val typeParamCount: Int,
    val memberCount: Int,
    val generatedLOC: Int,
    val analysisTimeNanos: Long,
    val generationTimeNanos: Long,
) {
    /**
     * Total time spent (analysis + generation).
     */
    val totalTimeNanos: Long
        get() = analysisTimeNanos + generationTimeNanos
}
