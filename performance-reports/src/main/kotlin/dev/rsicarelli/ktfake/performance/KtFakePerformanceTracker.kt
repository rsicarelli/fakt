// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance

import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Metro-inspired performance tracking for KtFakes compilation.
 *
 * Tracks compilation phases with detailed timing, memory usage, and metrics
 * to help identify performance bottlenecks in large projects.
 *
 * Example usage:
 * ```kotlin
 * val tracker = FaktPerformanceTracker.create()
 * tracker.startPhase(CompilationPhase.INTERFACE_DISCOVERY)
 * // ... do work
 * tracker.endPhase(CompilationPhase.INTERFACE_DISCOVERY, metadata = mapOf("interfaces_found" to 12))
 * ```
 */
class FaktPerformanceTracker private constructor(
    private val enabled: Boolean = true
) {
    companion object {
        fun create(enabled: Boolean = true): FaktPerformanceTracker {
            return FaktPerformanceTracker(enabled)
        }
    }

    // Performance measurement storage
    private val phaseTimings = ConcurrentHashMap<CompilationPhase, PhaseMetrics>()
    private val activePhases = ConcurrentHashMap<CompilationPhase, Long>()
    private val memoryMXBean = ManagementFactory.getMemoryMXBean()
    private val threadMXBean = ManagementFactory.getThreadMXBean()

    // Global compilation metrics
    private var compilationStartTime: Long = 0L
    private var peakMemoryUsage: Long = 0L
    private var totalFakesGenerated: Int = 0
    private var totalMethodsGenerated: Int = 0
    private var totalPropertiesGenerated: Int = 0

    /**
     * Start tracking a compilation phase.
     */
    fun startPhase(phase: CompilationPhase) {
        if (!enabled) return

        if (compilationStartTime == 0L) {
            compilationStartTime = System.nanoTime()
        }

        activePhases[phase] = System.nanoTime()
        updateMemoryUsage()
    }

    /**
     * End tracking a compilation phase with optional metadata.
     */
    fun endPhase(phase: CompilationPhase, metadata: Map<String, Any> = emptyMap()) {
        if (!enabled) return

        val startTime = activePhases.remove(phase) ?: return
        val endTime = System.nanoTime()
        val duration = endTime - startTime

        updateMemoryUsage()

        val metrics = PhaseMetrics(
            phase = phase,
            durationNanos = duration,
            memoryUsageMB = getCurrentMemoryUsage(),
            metadata = metadata
        )

        phaseTimings[phase] = metrics
    }

    /**
     * Add global compilation metrics.
     */
    fun recordGlobalMetrics(
        fakesGenerated: Int = 0,
        methodsGenerated: Int = 0,
        propertiesGenerated: Int = 0
    ) {
        if (!enabled) return

        totalFakesGenerated += fakesGenerated
        totalMethodsGenerated += methodsGenerated
        totalPropertiesGenerated += propertiesGenerated
    }

    /**
     * Generate a Metro-style performance report.
     */
    fun generateReport(): String {
        if (!enabled) return "Performance tracking disabled"

        val totalTime = if (compilationStartTime > 0) {
            (System.nanoTime() - compilationStartTime) / 1_000_000
        } else 0

        return buildString {
            appendLine("[KtFakeGeneration] ▶ Generate fake implementations")

            // Phase breakdown
            CompilationPhase.entries.forEach { phase ->
                val metrics = phaseTimings[phase]
                if (metrics != null) {
                    val timeMs = metrics.durationNanos / 1_000_000
                    val metadataStr = if (metrics.metadata.isNotEmpty()) {
                        ", ${formatMetadata(metrics.metadata)}"
                    } else ""

                    appendLine("  ▶ ${phase.displayName} (${timeMs}ms$metadataStr)")
                }
            }

            appendLine("[KtFakeGeneration] ◀ Total: ${totalTime}ms")
            appendLine()
            appendLine("Generation Summary:")
            appendLine("  - Fakes generated: $totalFakesGenerated")
            appendLine("  - Methods generated: $totalMethodsGenerated")
            appendLine("  - Properties generated: $totalPropertiesGenerated")
            appendLine("  - Peak memory usage: ${peakMemoryUsage}MB")

            // Performance insights
            appendLine()
            appendLine("Performance Insights:")
            generatePerformanceInsights().forEach { insight ->
                appendLine("  • $insight")
            }
        }
    }

    /**
     * Generate JSON report for programmatic consumption.
     */
    fun generateJsonReport(): String {
        if (!enabled) return """{"enabled": false}"""

        val totalTime = if (compilationStartTime > 0) {
            (System.nanoTime() - compilationStartTime) / 1_000_000
        } else 0

        val phases = phaseTimings.values.joinToString(",\n    ") { metrics ->
            val timeMs = metrics.durationNanos / 1_000_000
            val metadataJson = metrics.metadata.entries.joinToString(", ") { (k, v) ->
                "\"$k\": \"$v\""
            }

            """
    {
      "phase": "${metrics.phase.name}",
      "displayName": "${metrics.phase.displayName}",
      "durationMs": $timeMs,
      "memoryUsageMB": ${metrics.memoryUsageMB},
      "metadata": { $metadataJson }
    }""".trimIndent()
        }

        return """
{
  "enabled": true,
  "totalDurationMs": $totalTime,
  "peakMemoryUsageMB": $peakMemoryUsage,
  "summary": {
    "fakesGenerated": $totalFakesGenerated,
    "methodsGenerated": $totalMethodsGenerated,
    "propertiesGenerated": $totalPropertiesGenerated
  },
  "phases": [
    $phases
  ]
}""".trimIndent()
    }

    private fun updateMemoryUsage() {
        val currentMemory = getCurrentMemoryUsage()
        if (currentMemory > peakMemoryUsage) {
            peakMemoryUsage = currentMemory
        }
    }

    private fun getCurrentMemoryUsage(): Long {
        val memoryUsage = memoryMXBean.heapMemoryUsage
        return memoryUsage.used / (1024 * 1024) // Convert to MB
    }

    private fun formatMetadata(metadata: Map<String, Any>): String {
        return metadata.entries.joinToString(", ") { (key, value) ->
            when (key) {
                "interfaces_found", "methods_generated", "properties_generated" -> "$value $key"
                "cache_hit_rate" -> "${value}% cache hits"
                else -> "$key: $value"
            }
        }
    }

    private fun generatePerformanceInsights(): List<String> {
        val insights = mutableListOf<String>()

        // Analyze phase performance
        val typeAnalysisTime = phaseTimings[CompilationPhase.TYPE_ANALYSIS]?.durationNanos ?: 0
        val codeGenTime = phaseTimings[CompilationPhase.CODE_GENERATION]?.durationNanos ?: 0

        if (typeAnalysisTime > codeGenTime * 2) {
            insights.add("Type analysis is taking longer than code generation - consider caching")
        }

        if (peakMemoryUsage > 100) {
            insights.add("High memory usage detected - consider memory optimization")
        }

        if (totalFakesGenerated > 100) {
            insights.add("Large project detected - consider incremental compilation")
        }

        // Cache hit rate analysis
        val cacheMetrics = phaseTimings[CompilationPhase.TYPE_ANALYSIS]?.metadata?.get("cache_hit_rate")
        if (cacheMetrics is Number && cacheMetrics.toDouble() < 70.0) {
            insights.add("Low cache hit rate (${cacheMetrics}%) - cache strategy may need optimization")
        }

        if (insights.isEmpty()) {
            insights.add("Performance looks good! 🚀")
        }

        return insights
    }
}

/**
 * Compilation phases for detailed performance tracking.
 */
enum class CompilationPhase(val displayName: String) {
    INTERFACE_DISCOVERY("Discover @Fake interfaces"),
    PATTERN_ANALYSIS("Analyze interface patterns"),
    TYPE_ANALYSIS("Analyze type signatures"),
    CODE_GENERATION("Generate IR implementations"),
    FILE_OUTPUT("Write generated files"),
    TOTAL_COMPILATION("Total compilation")
}

/**
 * Metrics for a single compilation phase.
 */
data class PhaseMetrics(
    val phase: CompilationPhase,
    val durationNanos: Long,
    val memoryUsageMB: Long,
    val metadata: Map<String, Any>
)