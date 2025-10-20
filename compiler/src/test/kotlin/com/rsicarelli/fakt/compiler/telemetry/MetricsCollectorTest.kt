// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

import com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern
import com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics
import com.rsicarelli.fakt.compiler.telemetry.metrics.PhaseMetrics
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for MetricsCollector.
 *
 * Validates metric aggregation, counter management, and summary generation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetricsCollectorTest {
    @Test
    fun `GIVEN new collector WHEN checking initial state THEN all counters should be zero`() {
        // GIVEN
        val collector = MetricsCollector()

        // WHEN
        val interfacesDiscovered = collector.getInterfacesDiscovered()
        val interfacesProcessed = collector.getInterfacesProcessed()
        val interfacesCached = collector.getInterfacesCached()

        // THEN
        assertEquals(0, interfacesDiscovered)
        assertEquals(0, interfacesProcessed)
        assertEquals(0, interfacesCached)
    }

    @Test
    fun `GIVEN collector WHEN incrementing interfaces discovered THEN counter should increase`() {
        // GIVEN
        val collector = MetricsCollector()

        // WHEN
        collector.incrementInterfacesDiscovered(5)
        collector.incrementInterfacesDiscovered(3)

        // THEN
        assertEquals(8, collector.getInterfacesDiscovered())
    }

    @Test
    fun `GIVEN collector WHEN incrementing classes discovered THEN counter should increase`() {
        // GIVEN
        val collector = MetricsCollector()

        // WHEN
        collector.incrementClassesDiscovered(2)
        collector.incrementClassesProcessed(1)
        collector.incrementClassesCached(1)

        // THEN
        val summary =
            collector.buildSummary(
                totalTimeNanos = 100,
                phaseBreakdown = emptyMap(),
                outputDirectory = "test",
            )
        assertEquals(2, summary.classesDiscovered)
        assertEquals(1, summary.classesProcessed)
        assertEquals(1, summary.classesCached)
    }

    @Test
    fun `GIVEN collector WHEN recording interface metrics THEN aggregates should update`() {
        // GIVEN
        val collector = MetricsCollector()

        val metrics1 =
            FakeMetrics(
                name = "UserService",
                pattern = GenericPattern.NoGenerics,
                memberCount = 5,
                typeParamCount = 0,
                analysisTimeNanos = 10,
                generationTimeNanos = 20,
                generatedLOC = 50,
                fileSizeBytes = 1024,
                importCount = 3,
            )

        val metrics2 =
            FakeMetrics(
                name = "ProductService",
                pattern = GenericPattern.NoGenerics,
                memberCount = 3,
                typeParamCount = 0,
                analysisTimeNanos = 15,
                generationTimeNanos = 25,
                generatedLOC = 30,
                fileSizeBytes = 512,
                importCount = 2,
            )

        // WHEN
        collector.recordFakeMetrics(metrics1)
        collector.recordFakeMetrics(metrics2)

        // THEN
        val summary =
            collector.buildSummary(
                totalTimeNanos = 100,
                phaseBreakdown = emptyMap(),
                outputDirectory = "test",
            )

        assertEquals(80, summary.totalLOC) // 50 + 30
        assertEquals(2, summary.totalFiles)
        assertEquals(1536L, summary.totalFileSizeBytes) // 1024 + 512
        assertEquals(2, summary.fakeMetrics.size)
    }

    @Test
    fun `GIVEN collector with metrics WHEN building summary THEN summary should contain all data`() {
        // GIVEN
        val collector = MetricsCollector()
        collector.incrementInterfacesDiscovered(10)
        collector.incrementInterfacesProcessed(8)
        collector.incrementInterfacesCached(2)

        val phaseMetrics =
            PhaseMetrics(
                name = "DISCOVERY",
                startTime = 1000,
                endTime = 1050,
            )

        val interfaceMetrics =
            FakeMetrics(
                name = "TestInterface",
                pattern = GenericPattern.NoGenerics,
                memberCount = 3,
                typeParamCount = 0,
                analysisTimeNanos = 10,
                generationTimeNanos = 15,
                generatedLOC = 40,
                fileSizeBytes = 800,
                importCount = 2,
            )

        collector.recordFakeMetrics(interfaceMetrics)

        // WHEN
        val summary =
            collector.buildSummary(
                totalTimeNanos = 150,
                phaseBreakdown = mapOf("DISCOVERY" to phaseMetrics),
                outputDirectory = "build/generated/fakt/test",
            )

        // THEN
        assertEquals(150, summary.totalTimeNanos)
        assertEquals(10, summary.interfacesDiscovered)
        assertEquals(8, summary.interfacesProcessed)
        assertEquals(2, summary.interfacesCached)
        assertEquals(1, summary.phaseBreakdown.size)
        assertEquals(1, summary.fakeMetrics.size)
        assertEquals("build/generated/fakt/test", summary.outputDirectory)
    }

    @Test
    fun `GIVEN collector with data WHEN resetting THEN all counters should be zero`() {
        // GIVEN
        val collector = MetricsCollector()
        collector.incrementInterfacesDiscovered(5)
        collector.incrementInterfacesProcessed(3)
        collector.recordFakeMetrics(
            FakeMetrics(
                name = "Test",
                pattern = GenericPattern.NoGenerics,
                memberCount = 1,
                typeParamCount = 0,
                analysisTimeNanos = 5,
                generationTimeNanos = 10,
                generatedLOC = 20,
                fileSizeBytes = 400,
                importCount = 1,
            ),
        )

        // WHEN
        collector.reset()

        // THEN
        assertEquals(0, collector.getInterfacesDiscovered())
        assertEquals(0, collector.getInterfacesProcessed())
        assertEquals(0, collector.getInterfacesCached())
        assertTrue(collector.getFakeMetrics().isEmpty())
    }

    @Test
    fun `GIVEN collector WHEN concurrent increments THEN all updates should be counted`() =
        runTest {
            // GIVEN
            val collector = MetricsCollector()
            val incrementCount = 100

            // WHEN
            // Simulate concurrent updates from multiple threads
            repeat(incrementCount) {
                collector.incrementInterfacesDiscovered(1)
            }

            // THEN
            assertEquals(incrementCount, collector.getInterfacesDiscovered())
        }
}
