// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry.metrics

/**
 * Complete summary of a Fakt compilation session.
 *
 * Aggregates all metrics from a compilation run including phase timings,
 * fake statistics, cache performance, and code generation metrics.
 *
 * Used by [CompilationReport] to generate formatted output at different log levels.
 *
 * **Usage:**
 * ```kotlin
 * val summary = CompilationSummary(
 *     totalTimeMs = 1238,
 *     interfacesDiscovered = 15,
 *     interfacesProcessed = 10,
 *     interfacesCached = 5,
 *     classesDiscovered = 3,
 *     classesProcessed = 2,
 *     classesCached = 1,
 *     phaseBreakdown = mapOf(
 *         "DISCOVERY" to PhaseMetrics("DISCOVERY", start, start + 120),
 *         "ANALYSIS" to PhaseMetrics("ANALYSIS", start + 120, start + 460)
 *     ),
 *     fakeMetrics = listOf(...),
 *     totalLOC = 1847,
 *     totalFiles = 12,
 *     totalFileSizeBytes = 65432,
 *     outputDirectory = "build/generated/fakt/commonTest/kotlin"
 * )
 * ```
 *
 * @property totalTimeMs Total compilation time in milliseconds
 * @property interfacesDiscovered Total interfaces discovered with @Fake
 * @property interfacesProcessed Interfaces actually processed (not cached)
 * @property interfacesCached Interfaces skipped (cached, unchanged)
 * @property classesDiscovered Total classes discovered with @Fake
 * @property classesProcessed Classes actually processed (not cached)
 * @property classesCached Classes skipped (cached, unchanged)
 * @property phaseBreakdown Map of phase name to metrics
 * @property fakeMetrics Metrics for each processed fake (interface or class)
 * @property totalLOC Total lines of code generated
 * @property totalFiles Total number of files generated
 * @property totalFileSizeBytes Total size of generated files in bytes
 * @property outputDirectory Output directory path
 */
data class CompilationSummary(
    val totalTimeMs: Long,
    val interfacesDiscovered: Int,
    val interfacesProcessed: Int,
    val interfacesCached: Int,
    val classesDiscovered: Int = 0,
    val classesProcessed: Int = 0,
    val classesCached: Int = 0,
    val phaseBreakdown: Map<String, PhaseMetrics>,
    val fakeMetrics: List<FakeMetrics>,
    val totalLOC: Int,
    val totalFiles: Int,
    val totalFileSizeBytes: Long,
    val outputDirectory: String,
) {
    /**
     * Cache hit rate as percentage (0.0 - 100.0).
     *
     * Calculated as: (cached / discovered) * 100
     *
     * @return Cache hit rate percentage
     */
    fun cacheHitRate(): Double {
        val totalDiscovered = interfacesDiscovered + classesDiscovered
        if (totalDiscovered == 0) return 0.0
        val totalCached = interfacesCached + classesCached
        return (totalCached.toDouble() / totalDiscovered) * 100.0
    }

    /**
     * Average time per fake in milliseconds.
     *
     * @return Average processing time, or 0 if no fakes processed
     */
    fun avgTimePerFake(): Long {
        val totalProcessed = interfacesProcessed + classesProcessed
        if (totalProcessed == 0) return 0
        val totalFakeTime = fakeMetrics.sumOf { it.totalTimeMs }
        return totalFakeTime / totalProcessed
    }

    /**
     * Top N slowest fakes.
     *
     * @param n Number of slowest fakes to return (default: 3)
     * @return List of slowest fakes, sorted by total time descending
     */
    fun topSlowestFakes(n: Int = 3): List<FakeMetrics> = fakeMetrics.sortedByDescending { it.totalTimeMs }.take(n)

    /**
     * Formats total file size as human-readable string.
     *
     * Examples:
     * - 1024 bytes → "1.0 KB"
     * - 1048576 bytes → "1.0 MB"
     *
     * @return Formatted file size
     */
    fun formattedFileSize(): String =
        when {
            totalFileSizeBytes < 1024 -> "$totalFileSizeBytes B"
            totalFileSizeBytes < 1024 * 1024 -> String.format("%.1f KB", totalFileSizeBytes / 1024.0)
            else -> String.format("%.1f MB", totalFileSizeBytes / (1024.0 * 1024.0))
        }

    /**
     * Checks if compilation was successful (at least one fake generated).
     *
     * @return true if fakes were generated
     */
    fun isSuccessful(): Boolean = interfacesProcessed > 0 || classesProcessed > 0

    /**
     * Formats total time as human-readable string.
     *
     * Examples:
     * - 340ms → "340ms"
     * - 1238ms → "1.2s"
     *
     * @return Formatted total time
     */
    fun formattedTotalTime(): String =
        when {
            totalTimeMs < 1000 -> "${totalTimeMs}ms"
            else -> String.format("%.1fs", totalTimeMs / 1000.0)
        }

    /**
     * Total number of fakes generated (interfaces + classes).
     *
     * @return Sum of interfaces and classes processed
     */
    fun totalProcessed(): Int = interfacesProcessed + classesProcessed

    /**
     * Total number of items discovered (interfaces + classes).
     *
     * @return Sum of interfaces and classes discovered
     */
    fun totalDiscovered(): Int = interfacesDiscovered + classesDiscovered

    /**
     * Number of new fakes generated (not from cache).
     *
     * Calculated as: totalProcessed = processed (new) + cached
     * So: new = processed (since processed means "newly generated")
     *
     * @return Number of fakes that were freshly generated
     */
    fun newFakes(): Int = interfacesProcessed + classesProcessed

    /**
     * Number of fakes reused from cache.
     *
     * @return Number of fakes that were cached and not regenerated
     */
    fun cachedFakes(): Int = interfacesCached + classesCached

    /**
     * Formats a number with thousand separators.
     *
     * Examples:
     * - 1234 → "1,234"
     * - 65432 → "65,432"
     * - 100 → "100"
     *
     * @param number The number to format
     * @return Formatted number string
     */
    fun formatNumber(number: Int): String = String.format("%,d", number)

    /**
     * Average lines of code per file.
     *
     * @return Average LOC, or 0 if no files generated
     */
    fun avgLOCPerFile(): Int =
        if (totalFiles == 0) {
            0
        } else {
            totalLOC / totalFiles
        }

    /**
     * Gets phase timing for a specific phase.
     *
     * @param phaseName Name of the phase (e.g., "DISCOVERY", "GENERATION")
     * @return Phase metrics, or null if phase not found
     */
    fun getPhase(phaseName: String): PhaseMetrics? = phaseBreakdown[phaseName]

    /**
     * Gets discovery phase timing.
     *
     * @return Discovery phase metrics, or 0ms if not found
     */
    fun discoveryTimeMs(): Long = getPhase("DISCOVERY")?.duration ?: 0L

    /**
     * Gets generation phase timing.
     *
     * @return Generation phase metrics, or 0ms if not found
     */
    fun generationTimeMs(): Long = getPhase("GENERATION")?.duration ?: 0L
}
