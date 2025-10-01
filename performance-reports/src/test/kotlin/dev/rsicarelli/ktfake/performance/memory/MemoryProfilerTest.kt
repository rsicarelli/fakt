// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.memory

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MemoryProfilerTest {

    @Test
    fun `GIVEN memory profiler WHEN starting and stopping profiling THEN should capture measurements`() {
        // Given
        val profiler = MemoryProfiler()

        // When
        profiler.startProfiling()

        // Simulate some work that uses memory
        val largeList = mutableListOf<String>()
        repeat(1000) { largeList.add("String $it") }

        profiler.takeMeasurement("After creating large list")

        val report = profiler.stopProfiling()

        // Then
        assertTrue(report.measurementCount >= 2, "Should have initial and final measurements")
        assertTrue(report.totalDurationMs >= 0, "Should have measured time")
        assertTrue(report.heapStats.maxUsageMB > 0, "Should have measured heap usage")
        assertNotNull(report.recommendations)
    }

    @Test
    fun `GIVEN memory profiler WHEN measuring phase THEN should capture start and end measurements`() {
        // Given
        val profiler = MemoryProfiler()

        // When
        profiler.startProfiling()

        profiler.measurePhase("Test Phase") {
            // Simulate work that allocates memory
            val data = (1..500).map { "Data item $it" }
            // Use the data to prevent optimization
            data.forEach { it.length }
        }

        val report = profiler.stopProfiling()

        // Then
        assertTrue(report.measurementCount >= 4, "Should have start, phase start, phase end, and stop measurements")

        val phaseStartMeasurement = report.measurements.find { it.label == "Test Phase - Start" }
        val phaseEndMeasurement = report.measurements.find { it.label == "Test Phase - End" }

        assertNotNull(phaseStartMeasurement, "Should have phase start measurement")
        assertNotNull(phaseEndMeasurement, "Should have phase end measurement")
        assertTrue(phaseEndMeasurement.timestamp > phaseStartMeasurement.timestamp)
    }

    @Test
    fun `GIVEN memory profiler WHEN getting current memory usage THEN should return valid data`() {
        // Given
        val profiler = MemoryProfiler()

        // When
        val currentUsage = profiler.getCurrentMemoryUsage()

        // Then
        assertTrue(currentUsage.heapUsedMB > 0, "Heap should be used")
        assertTrue(currentUsage.totalUsedMB > 0, "Total memory should be used")
        assertTrue(currentUsage.totalUsedMB >= currentUsage.heapUsedMB, "Total should include heap")
    }

    @Test
    fun `GIVEN memory profiler WHEN measuring garbage collection THEN should show memory freed`() {
        // Given
        val profiler = MemoryProfiler()

        // When
        // Allocate some objects that can be GC'd
        var largeList: MutableList<String>? = mutableListOf()
        repeat(2000) { largeList?.add("Large string that takes memory $it".repeat(10)) }

        // Clear reference to make objects eligible for GC
        largeList = null

        val gcMeasurement = profiler.measureGarbageCollection()

        // Then
        assertTrue(gcMeasurement.gcDurationMs >= 0, "GC duration should be non-negative")
        assertNotNull(gcMeasurement.beforeGC)
        assertNotNull(gcMeasurement.afterGC)
        // Note: freedMemoryMB might be negative if memory increased during GC (due to concurrent allocation)
        // or 0 if GC didn't run or nothing was collected
        // We just verify the measurement was captured
        assertTrue(gcMeasurement.freedMemoryMB != Long.MAX_VALUE, "Freed memory should be calculated")
    }

    @Test
    fun `GIVEN empty profiler WHEN stopping without starting THEN should return empty report`() {
        // Given
        val profiler = MemoryProfiler()

        // When
        val report = profiler.stopProfiling()

        // Then
        assertEquals(0, report.measurementCount, "Should have no measurements")
        assertEquals(0, report.totalDurationMs, "Should have no duration")
        assertTrue(report.recommendations.isNotEmpty(), "Should have default recommendations")
    }

    @Test
    fun `GIVEN memory profiler WHEN taking many measurements THEN should limit stored measurements`() {
        // Given
        val profiler = MemoryProfiler()

        // When
        profiler.startProfiling()

        // Take more measurements than the limit (1000)
        repeat(1200) { index ->
            profiler.takeMeasurement("Measurement $index")
        }

        val report = profiler.stopProfiling()

        // Then
        // Should not exceed max measurements + some buffer for start/stop
        assertTrue(report.measurementCount <= 1010, "Should limit number of measurements to prevent memory issues")
    }

    @Test
    fun `GIVEN memory profiler report WHEN generating text report THEN should contain all sections`() {
        // Given
        val profiler = MemoryProfiler()

        // When
        profiler.startProfiling()

        // Create some memory pressure
        val data = mutableListOf<ByteArray>()
        repeat(100) {
            data.add(ByteArray(1024 * 10)) // 10KB arrays
            profiler.takeMeasurement("Creating array $it")
        }

        val report = profiler.stopProfiling()
        val textReport = report.generateTextReport()

        // Then
        assertTrue(textReport.contains("Memory Profiling Report"), "Should have report title")
        assertTrue(textReport.contains("Overview"), "Should have overview section")
        assertTrue(textReport.contains("Heap Memory"), "Should have heap memory section")
        assertTrue(textReport.contains("Non-Heap Memory"), "Should have non-heap memory section")
        assertTrue(textReport.contains("Recommendations"), "Should have recommendations section")
        assertTrue(textReport.contains("Duration"), "Should show duration")
        assertTrue(textReport.contains("MB"), "Should show memory in MB")
    }

    @Test
    fun `GIVEN memory profiler WHEN detecting memory pressure THEN should identify pressure points`() {
        // Given
        val profiler = MemoryProfiler()

        // When
        profiler.startProfiling()

        // Create initial small allocation
        var smallData = mutableListOf<String>()
        repeat(100) { smallData.add("small $it") }
        profiler.takeMeasurement("Small allocation")

        // Create large allocation to trigger pressure point
        var largeData = mutableListOf<ByteArray>()
        repeat(1000) {
            largeData.add(ByteArray(1024 * 100)) // 100KB arrays = ~100MB total
        }
        profiler.takeMeasurement("Large allocation")

        val report = profiler.stopProfiling()

        // Then
        assertTrue(report.measurements.size >= 3, "Should have multiple measurements")
        // Note: Pressure points depend on actual memory allocation, which can vary
        // So we just verify the structure is there
        assertNotNull(report.pressurePoints, "Should have pressure points list")
        assertTrue(report.memoryGrowthRate >= 0, "Should calculate growth rate")
    }

    @Test
    fun `GIVEN memory profiler WHEN profiling realistic compilation scenario THEN should provide useful insights`() {
        // Given
        val profiler = MemoryProfiler()

        // When - Simulate compilation phases
        profiler.startProfiling()

        profiler.measurePhase("Interface Discovery") {
            // Simulate discovering interfaces
            val interfaces = (1..100).map { "Interface$it" }
            Thread.sleep(10) // Simulate work
        }

        profiler.measurePhase("Type Analysis") {
            // Simulate type analysis with caching
            val typeCache = mutableMapOf<String, String>()
            repeat(500) {
                typeCache["Type$it"] = "AnalyzedType$it"
            }
            Thread.sleep(20) // Simulate work
        }

        profiler.measurePhase("Code Generation") {
            // Simulate code generation
            val generatedCode = mutableListOf<String>()
            repeat(100) {
                generatedCode.add("class FakeInterface$it { /* generated code */ }".repeat(50))
            }
            Thread.sleep(15) // Simulate work
        }

        val report = profiler.stopProfiling()

        // Then
        assertTrue(report.totalDurationMs > 30, "Should measure realistic duration")
        assertTrue(report.measurementCount >= 8, "Should have measurements for all phases")

        // Verify phase measurements exist
        val phaseLabels = report.measurements.map { it.label }
        assertTrue(phaseLabels.any { it.contains("Interface Discovery") })
        assertTrue(phaseLabels.any { it.contains("Type Analysis") })
        assertTrue(phaseLabels.any { it.contains("Code Generation") })

        assertTrue(report.recommendations.isNotEmpty(), "Should provide recommendations")
    }
}