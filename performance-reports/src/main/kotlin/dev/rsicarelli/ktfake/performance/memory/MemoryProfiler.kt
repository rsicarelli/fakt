// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.memory

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * Memory profiler for KtFakes compilation process.
 *
 * Tracks memory usage patterns, peak consumption, and provides insights
 * for memory optimization. Based on JVM memory management APIs.
 */
class MemoryProfiler {

    private val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    private val measurements = ConcurrentLinkedQueue<MemoryMeasurement>()
    private val peakHeapUsage = AtomicLong(0)
    private val peakNonHeapUsage = AtomicLong(0)
    private var startTime = 0L
    private var isEnabled = false

    companion object {
        private const val MAX_MEASUREMENTS = 1000 // Prevent memory leak from measurements
    }

    /**
     * Start memory profiling session.
     */
    fun startProfiling() {
        isEnabled = true
        startTime = System.currentTimeMillis()
        measurements.clear()
        peakHeapUsage.set(0)
        peakNonHeapUsage.set(0)

        // Take initial measurement
        takeMeasurement("Profiling started")
    }

    /**
     * Stop memory profiling and return final report.
     */
    fun stopProfiling(): MemoryProfilingReport {
        if (!isEnabled) {
            return MemoryProfilingReport.empty()
        }

        takeMeasurement("Profiling stopped")
        isEnabled = false

        val totalDuration = System.currentTimeMillis() - startTime
        val measurementsList = measurements.toList()

        return generateReport(measurementsList, totalDuration)
    }

    /**
     * Take a memory measurement with optional label.
     */
    fun takeMeasurement(label: String = "Measurement") {
        if (!isEnabled) return

        val heapUsage = memoryMXBean.heapMemoryUsage
        val nonHeapUsage = memoryMXBean.nonHeapMemoryUsage
        val timestamp = System.currentTimeMillis() - startTime

        val measurement = MemoryMeasurement(
            timestamp = timestamp,
            label = label,
            heapUsed = heapUsage.used,
            heapMax = heapUsage.max,
            heapCommitted = heapUsage.committed,
            nonHeapUsed = nonHeapUsage.used,
            nonHeapMax = nonHeapUsage.max,
            nonHeapCommitted = nonHeapUsage.committed
        )

        measurements.offer(measurement)

        // Update peaks
        peakHeapUsage.updateAndGet { current -> max(current, heapUsage.used) }
        peakNonHeapUsage.updateAndGet { current -> max(current, nonHeapUsage.used) }

        // Prevent memory leak - remove oldest measurements if we have too many
        while (measurements.size > MAX_MEASUREMENTS) {
            measurements.poll()
        }
    }

    /**
     * Take measurement for a specific compilation phase.
     */
    fun measurePhase(phaseName: String, block: () -> Unit) {
        takeMeasurement("$phaseName - Start")

        try {
            block()
        } finally {
            takeMeasurement("$phaseName - End")
        }
    }

    /**
     * Get current memory usage without storing measurement.
     */
    fun getCurrentMemoryUsage(): CurrentMemoryUsage {
        val heapUsage = memoryMXBean.heapMemoryUsage
        val nonHeapUsage = memoryMXBean.nonHeapMemoryUsage

        return CurrentMemoryUsage(
            heapUsedMB = heapUsage.used / (1024 * 1024),
            heapMaxMB = if (heapUsage.max > 0) heapUsage.max / (1024 * 1024) else -1,
            nonHeapUsedMB = nonHeapUsage.used / (1024 * 1024),
            totalUsedMB = (heapUsage.used + nonHeapUsage.used) / (1024 * 1024)
        )
    }

    /**
     * Force garbage collection and measure the effect.
     */
    fun measureGarbageCollection(): GCMeasurement {
        val beforeGC = getCurrentMemoryUsage()
        takeMeasurement("Before GC")

        val gcStartTime = System.currentTimeMillis()
        System.gc()
        Thread.sleep(100) // Give GC time to complete
        val gcDuration = System.currentTimeMillis() - gcStartTime

        val afterGC = getCurrentMemoryUsage()
        takeMeasurement("After GC")

        return GCMeasurement(
            beforeGC = beforeGC,
            afterGC = afterGC,
            freedMemoryMB = beforeGC.totalUsedMB - afterGC.totalUsedMB,
            gcDurationMs = gcDuration
        )
    }

    private fun generateReport(
        measurements: List<MemoryMeasurement>,
        totalDurationMs: Long
    ): MemoryProfilingReport {
        if (measurements.isEmpty()) {
            return MemoryProfilingReport.empty()
        }

        val heapUsages = measurements.map { it.heapUsed }
        val nonHeapUsages = measurements.map { it.nonHeapUsed }

        val minHeapUsage = heapUsages.minOrNull() ?: 0
        val maxHeapUsage = heapUsages.maxOrNull() ?: 0
        val avgHeapUsage = heapUsages.average().toLong()

        val minNonHeapUsage = nonHeapUsages.minOrNull() ?: 0
        val maxNonHeapUsage = nonHeapUsages.maxOrNull() ?: 0
        val avgNonHeapUsage = nonHeapUsages.average().toLong()

        // Analyze memory growth patterns
        val memoryGrowth = analyzeMemoryGrowth(measurements)
        val memoryPressurePoints = findMemoryPressurePoints(measurements)

        return MemoryProfilingReport(
            totalDurationMs = totalDurationMs,
            measurementCount = measurements.size,
            heapStats = MemoryStats(
                minUsageMB = minHeapUsage / (1024 * 1024),
                maxUsageMB = maxHeapUsage / (1024 * 1024),
                avgUsageMB = avgHeapUsage / (1024 * 1024),
                peakUsageMB = peakHeapUsage.get() / (1024 * 1024)
            ),
            nonHeapStats = MemoryStats(
                minUsageMB = minNonHeapUsage / (1024 * 1024),
                maxUsageMB = maxNonHeapUsage / (1024 * 1024),
                avgUsageMB = avgNonHeapUsage / (1024 * 1024),
                peakUsageMB = peakNonHeapUsage.get() / (1024 * 1024)
            ),
            memoryGrowthRate = memoryGrowth,
            pressurePoints = memoryPressurePoints,
            measurements = measurements,
            recommendations = generateRecommendations(measurements, memoryGrowth)
        )
    }

    private fun analyzeMemoryGrowth(measurements: List<MemoryMeasurement>): Double {
        if (measurements.size < 2) return 0.0

        val first = measurements.first()
        val last = measurements.last()
        val totalGrowth = (last.heapUsed + last.nonHeapUsed) - (first.heapUsed + first.nonHeapUsed)
        val timeSpan = last.timestamp - first.timestamp

        return if (timeSpan > 0) {
            totalGrowth.toDouble() / timeSpan.toDouble() // bytes per ms
        } else 0.0
    }

    private fun findMemoryPressurePoints(measurements: List<MemoryMeasurement>): List<MemoryPressurePoint> {
        val pressurePoints = mutableListOf<MemoryPressurePoint>()

        for (i in 1 until measurements.size) {
            val current = measurements[i]
            val previous = measurements[i-1]

            val heapGrowth = current.heapUsed - previous.heapUsed
            val heapGrowthMB = heapGrowth / (1024 * 1024)

            // Consider significant growth (>50MB) as a pressure point
            if (heapGrowthMB > 50) {
                pressurePoints.add(
                    MemoryPressurePoint(
                        timestamp = current.timestamp,
                        label = current.label,
                        memoryGrowthMB = heapGrowthMB,
                        totalMemoryMB = (current.heapUsed + current.nonHeapUsed) / (1024 * 1024)
                    )
                )
            }
        }

        return pressurePoints
    }

    private fun generateRecommendations(
        measurements: List<MemoryMeasurement>,
        growthRate: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        val maxHeapMB = measurements.maxOfOrNull { it.heapUsed }?.let { it / (1024 * 1024) } ?: 0
        val avgHeapMB = measurements.map { it.heapUsed }.average() / (1024 * 1024)

        // High memory usage recommendations
        if (maxHeapMB > 1000) {
            recommendations.add("High peak memory usage (${maxHeapMB}MB) - consider implementing object pooling")
        }

        // Memory growth recommendations
        if (growthRate > 1000) { // More than 1KB per ms growth
            recommendations.add("High memory growth rate detected - review for memory leaks")
        }

        // Memory efficiency recommendations
        if (maxHeapMB > avgHeapMB * 2) {
            recommendations.add("Large memory spikes detected - consider batch processing for large operations")
        }

        // General recommendations
        if (maxHeapMB > 500) {
            recommendations.add("Consider enabling incremental compilation to reduce memory pressure")
            recommendations.add("Use string interning for frequently used type names")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Memory usage looks optimal! 🚀")
        }

        return recommendations
    }
}

// Data classes for memory profiling
data class MemoryMeasurement(
    val timestamp: Long,
    val label: String,
    val heapUsed: Long,
    val heapMax: Long,
    val heapCommitted: Long,
    val nonHeapUsed: Long,
    val nonHeapMax: Long,
    val nonHeapCommitted: Long
)

data class CurrentMemoryUsage(
    val heapUsedMB: Long,
    val heapMaxMB: Long,
    val nonHeapUsedMB: Long,
    val totalUsedMB: Long
)

data class GCMeasurement(
    val beforeGC: CurrentMemoryUsage,
    val afterGC: CurrentMemoryUsage,
    val freedMemoryMB: Long,
    val gcDurationMs: Long
)

data class MemoryStats(
    val minUsageMB: Long,
    val maxUsageMB: Long,
    val avgUsageMB: Long,
    val peakUsageMB: Long
)

data class MemoryPressurePoint(
    val timestamp: Long,
    val label: String,
    val memoryGrowthMB: Long,
    val totalMemoryMB: Long
)

data class MemoryProfilingReport(
    val totalDurationMs: Long,
    val measurementCount: Int,
    val heapStats: MemoryStats,
    val nonHeapStats: MemoryStats,
    val memoryGrowthRate: Double,
    val pressurePoints: List<MemoryPressurePoint>,
    val measurements: List<MemoryMeasurement>,
    val recommendations: List<String>
) {
    companion object {
        fun empty() = MemoryProfilingReport(
            totalDurationMs = 0,
            measurementCount = 0,
            heapStats = MemoryStats(0, 0, 0, 0),
            nonHeapStats = MemoryStats(0, 0, 0, 0),
            memoryGrowthRate = 0.0,
            pressurePoints = emptyList(),
            measurements = emptyList(),
            recommendations = listOf("No memory profiling data available")
        )
    }

    fun generateTextReport(): String {
        return buildString {
            appendLine("🧠 KtFakes Memory Profiling Report")
            appendLine("=" * 40)
            appendLine()
            appendLine("📊 Overview:")
            appendLine("  • Duration: ${totalDurationMs}ms")
            appendLine("  • Measurements: $measurementCount")
            appendLine("  • Memory growth rate: ${String.format("%.2f", memoryGrowthRate)} bytes/ms")
            appendLine()
            appendLine("💾 Heap Memory:")
            appendLine("  • Min usage: ${heapStats.minUsageMB}MB")
            appendLine("  • Max usage: ${heapStats.maxUsageMB}MB")
            appendLine("  • Avg usage: ${heapStats.avgUsageMB}MB")
            appendLine("  • Peak usage: ${heapStats.peakUsageMB}MB")
            appendLine()
            appendLine("🔧 Non-Heap Memory:")
            appendLine("  • Min usage: ${nonHeapStats.minUsageMB}MB")
            appendLine("  • Max usage: ${nonHeapStats.maxUsageMB}MB")
            appendLine("  • Avg usage: ${nonHeapStats.avgUsageMB}MB")
            appendLine("  • Peak usage: ${nonHeapStats.peakUsageMB}MB")

            if (pressurePoints.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ Memory Pressure Points:")
                pressurePoints.forEach { point ->
                    appendLine("  • ${point.timestamp}ms: ${point.label} (+${point.memoryGrowthMB}MB, total: ${point.totalMemoryMB}MB)")
                }
            }

            appendLine()
            appendLine("💡 Recommendations:")
            recommendations.forEach { recommendation ->
                appendLine("  • $recommendation")
            }
        }
    }

    private operator fun String.times(n: Int): String = repeat(n)
}