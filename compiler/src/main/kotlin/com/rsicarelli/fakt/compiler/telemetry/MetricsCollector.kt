// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

import com.rsicarelli.fakt.compiler.telemetry.metrics.CompilationSummary
import com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics
import com.rsicarelli.fakt.compiler.telemetry.metrics.PhaseMetrics
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Collects and aggregates metrics during compilation.
 *
 * **Thread-safe:** Uses atomic operations for concurrent metric updates.
 *
 * Tracks:
 * - Counters (interfaces/classes discovered, processed, cached)
 * - Timings (phase breakdown, per-fake metrics)
 * - Sizes (LOC generated, file sizes)
 * - Rates (cache hit rate, avg time per fake)
 *
 * **Usage:**
 * ```kotlin
 * val collector = MetricsCollector()
 *
 * collector.incrementInterfacesDiscovered(15)
 * collector.incrementInterfacesProcessed(10)
 * collector.incrementInterfacesCached(5)
 *
 * collector.recordFakeMetrics(
 *     FakeMetrics(
 *         name = "UserService",
 *         pattern = GenericPattern.NoGenerics,
 *         memberCount = 5,
 *         typeParamCount = 0,
 *         analysisTimeMs = 18,
 *         generationTimeMs = 45,
 *         generatedLOC = 87,
 *         importCount = 3
 *     )
 * )
 *
 * val summary = collector.buildSummary(
 *     totalTimeMs = 1238,
 *     phaseBreakdown = mapOf("DISCOVERY" to discoveryMetrics),
 *     outputDirectory = "build/generated/fakt/commonTest/kotlin"
 * )
 * ```
 *
 * @see CompilationSummary
 * @see FakeMetrics
 */
class MetricsCollector {
    // Counters (thread-safe)
    private val interfacesDiscovered = AtomicInteger(0)
    private val interfacesProcessed = AtomicInteger(0)
    private val interfacesCached = AtomicInteger(0)
    private val classesDiscovered = AtomicInteger(0)
    private val classesProcessed = AtomicInteger(0)
    private val classesCached = AtomicInteger(0)

    // Sizes (thread-safe)
    private val totalLOC = AtomicInteger(0)
    private val totalFiles = AtomicInteger(0)
    private val totalFileSizeBytes = AtomicLong(0)

    // Per-fake metrics (synchronized list for thread-safety)
    private val fakeMetrics = mutableListOf<FakeMetrics>()

    /**
     * Increments the count of discovered interfaces.
     *
     * @param count Number of interfaces discovered (default: 1)
     */
    fun incrementInterfacesDiscovered(count: Int = 1) {
        interfacesDiscovered.addAndGet(count)
    }

    /**
     * Increments the count of processed interfaces.
     *
     * @param count Number of interfaces processed (default: 1)
     */
    fun incrementInterfacesProcessed(count: Int = 1) {
        interfacesProcessed.addAndGet(count)
    }

    /**
     * Increments the count of cached interfaces.
     *
     * @param count Number of interfaces cached (default: 1)
     */
    fun incrementInterfacesCached(count: Int = 1) {
        interfacesCached.addAndGet(count)
    }

    /**
     * Increments the count of discovered classes.
     *
     * @param count Number of classes discovered (default: 1)
     */
    fun incrementClassesDiscovered(count: Int = 1) {
        classesDiscovered.addAndGet(count)
    }

    /**
     * Increments the count of processed classes.
     *
     * @param count Number of classes processed (default: 1)
     */
    fun incrementClassesProcessed(count: Int = 1) {
        classesProcessed.addAndGet(count)
    }

    /**
     * Increments the count of cached classes.
     *
     * @param count Number of classes cached (default: 1)
     */
    fun incrementClassesCached(count: Int = 1) {
        classesCached.addAndGet(count)
    }

    /**
     * Records metrics for a single fake (interface or class).
     *
     * Also updates aggregate metrics (total LOC, files, file size).
     *
     * @param metrics The fake metrics to record
     */
    @Synchronized
    fun recordFakeMetrics(metrics: FakeMetrics) {
        fakeMetrics.add(metrics)
        totalLOC.addAndGet(metrics.generatedLOC)
        totalFiles.incrementAndGet()
        totalFileSizeBytes.addAndGet(metrics.fileSizeBytes.toLong())
    }

    /**
     * Adds to the total file size counter.
     *
     * @param bytes Size of generated file in bytes
     */
    fun addFileSize(bytes: Long) {
        totalFileSizeBytes.addAndGet(bytes)
    }

    /**
     * Builds a complete compilation summary from collected metrics.
     *
     * @param totalTimeMs Total compilation time in milliseconds
     * @param phaseBreakdown Map of phase name to metrics
     * @param outputDirectory Output directory path
     * @return Complete compilation summary
     */
    @Synchronized
    fun buildSummary(
        totalTimeMs: Long,
        phaseBreakdown: Map<String, PhaseMetrics>,
        outputDirectory: String,
    ): CompilationSummary =
        CompilationSummary(
            totalTimeMs = totalTimeMs,
            interfacesDiscovered = interfacesDiscovered.get(),
            interfacesProcessed = interfacesProcessed.get(),
            interfacesCached = interfacesCached.get(),
            classesDiscovered = classesDiscovered.get(),
            classesProcessed = classesProcessed.get(),
            classesCached = classesCached.get(),
            phaseBreakdown = phaseBreakdown,
            fakeMetrics = fakeMetrics.toList(), // Defensive copy
            totalLOC = totalLOC.get(),
            totalFiles = totalFiles.get(),
            totalFileSizeBytes = totalFileSizeBytes.get(),
            outputDirectory = outputDirectory,
        )

    /**
     * Resets all metrics to zero.
     *
     * Useful for testing or starting a fresh compilation session.
     */
    @Synchronized
    fun reset() {
        interfacesDiscovered.set(0)
        interfacesProcessed.set(0)
        interfacesCached.set(0)
        classesDiscovered.set(0)
        classesProcessed.set(0)
        classesCached.set(0)
        totalLOC.set(0)
        totalFiles.set(0)
        totalFileSizeBytes.set(0)
        fakeMetrics.clear()
    }

    /**
     * Gets current snapshot of fake metrics.
     *
     * @return Defensive copy of fake metrics list
     */
    @Synchronized
    fun getFakeMetrics(): List<FakeMetrics> = fakeMetrics.toList()

    /**
     * Gets current count of discovered interfaces.
     */
    fun getInterfacesDiscovered(): Int = interfacesDiscovered.get()

    /**
     * Gets current count of processed interfaces.
     */
    fun getInterfacesProcessed(): Int = interfacesProcessed.get()

    /**
     * Gets current count of cached interfaces.
     */
    fun getInterfacesCached(): Int = interfacesCached.get()
}
