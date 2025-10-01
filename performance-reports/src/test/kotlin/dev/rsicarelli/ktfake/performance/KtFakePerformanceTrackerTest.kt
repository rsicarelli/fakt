// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertContains

class FaktPerformanceTrackerTest {

    @Test
    fun `GIVEN performance tracker WHEN tracking compilation phases THEN should generate report`() {
        // Given
        val tracker = FaktPerformanceTracker.create(enabled = true)

        // When
        tracker.startPhase(CompilationPhase.INTERFACE_DISCOVERY)
        Thread.sleep(10) // Simulate work
        tracker.endPhase(CompilationPhase.INTERFACE_DISCOVERY, mapOf("interfaces_found" to 5))

        tracker.startPhase(CompilationPhase.CODE_GENERATION)
        Thread.sleep(10) // Simulate work
        tracker.endPhase(CompilationPhase.CODE_GENERATION, mapOf("methods_generated" to 25))

        tracker.recordGlobalMetrics(fakesGenerated = 5, methodsGenerated = 25, propertiesGenerated = 10)

        // Then
        val report = tracker.generateReport()
        assertContains(report, "[KtFakeGeneration]")
        assertContains(report, "Discover @Fake interfaces")
        assertContains(report, "Generate IR implementations")
        assertContains(report, "5 interfaces_found")
        assertContains(report, "25 methods_generated")
        assertContains(report, "Fakes generated: 5")
    }

    @Test
    fun `GIVEN disabled tracker WHEN generating report THEN should indicate disabled`() {
        // Given
        val tracker = FaktPerformanceTracker.create(enabled = false)

        // When
        tracker.startPhase(CompilationPhase.INTERFACE_DISCOVERY)
        tracker.endPhase(CompilationPhase.INTERFACE_DISCOVERY)
        val report = tracker.generateReport()

        // Then
        assertContains(report, "Performance tracking disabled")
    }

    @Test
    fun `GIVEN performance tracker WHEN generating JSON report THEN should produce valid JSON structure`() {
        // Given
        val tracker = FaktPerformanceTracker.create(enabled = true)

        // When
        tracker.startPhase(CompilationPhase.TYPE_ANALYSIS)
        Thread.sleep(5)
        tracker.endPhase(CompilationPhase.TYPE_ANALYSIS, mapOf("cache_hit_rate" to 85))

        val jsonReport = tracker.generateJsonReport()

        // Then
        assertContains(jsonReport, "\"enabled\": true")
        assertContains(jsonReport, "\"totalDurationMs\":")
        assertContains(jsonReport, "\"fakesGenerated\":")
        assertContains(jsonReport, "\"phases\":")
        assertContains(jsonReport, "\"TYPE_ANALYSIS\"")
        assertContains(jsonReport, "\"cache_hit_rate\": \"85\"")
    }
}