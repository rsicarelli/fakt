// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern
import com.rsicarelli.fakt.compiler.telemetry.metrics.CompilationSummary
import com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics
import com.rsicarelli.fakt.compiler.telemetry.metrics.PhaseMetrics
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for CompilationReport.
 *
 * Validates report generation API and delegation to ReportFormatter.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilationReportTest {
    private fun createTestSummary(
        totalTimeMs: Long = 44,
        interfacesProcessed: Int = 100,
        classesProcessed: Int = 21,
    ): CompilationSummary {
        val phaseMetrics = PhaseMetrics("GENERATION", 1000, 1044)

        val fakeMetrics =
            FakeMetrics(
                name = "UserService",
                pattern = GenericPattern.NoGenerics,
                memberCount = 5,
                typeParamCount = 0,
                analysisTimeMs = 10,
                generationTimeMs = 20,
                generatedLOC = 87,
                fileSizeBytes = 2450,
                importCount = 3,
            )

        return CompilationSummary(
            totalTimeMs = totalTimeMs,
            interfacesDiscovered = interfacesProcessed,
            interfacesProcessed = interfacesProcessed,
            interfacesCached = 0,
            classesDiscovered = classesProcessed,
            classesProcessed = classesProcessed,
            classesCached = 0,
            phaseBreakdown = mapOf("GENERATION" to phaseMetrics),
            fakeMetrics = listOf(fakeMetrics),
            totalLOC = 1234,
            totalFiles = 121,
            totalFileSizeBytes = 65432,
            outputDirectory = "build/generated/fakt/test/kotlin",
        )
    }

    @Test
    fun `GIVEN summary WHEN generating with QUIET level THEN should return empty string`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = CompilationReport.generate(summary, LogLevel.QUIET)

        // THEN
        assertEquals("", report)
    }

    @Test
    fun `GIVEN summary WHEN generating with INFO level THEN should return single line summary`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = CompilationReport.generate(summary, LogLevel.INFO)

        // THEN
        assertTrue(report.contains("121 fakes"))
        assertTrue(report.contains("44ms"))
        assertEquals(1, report.lines().size, "INFO should be a single line")
    }

    @Test
    fun `GIVEN summary WHEN generating with DEBUG level THEN should return multi-line compact report`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = CompilationReport.generate(summary, LogLevel.DEBUG)

        // THEN
        assertTrue(report.contains("Discovery:"))
        assertTrue(report.contains("Generation:"))
        val lines = report.lines().filter { it.isNotBlank() }
        assertEquals(3, lines.size, "DEBUG should have 3 clean lines, got ${lines.size}")
    }

    @Test
    fun `GIVEN summary WHEN generating with TRACE level THEN should return exhaustive report`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = CompilationReport.generate(summary, LogLevel.TRACE)

        // THEN
        assertTrue(report.contains("FAKT COMPILATION REPORT (TRACE)"))
        val lines = report.lines().filter { it.isNotBlank() }
        assertTrue(lines.size > 20, "TRACE should have 20+ lines, got ${lines.size}")
    }

    @Test
    fun `GIVEN summary WHEN generating lines with INFO THEN should split into list`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val lines = CompilationReport.generateLines(summary, LogLevel.INFO)

        // THEN
        assertEquals(1, lines.size, "INFO generates 1 line")
        assertTrue(lines.first().contains("✅"))
    }

    @Test
    fun `GIVEN successful compilation WHEN generating success message THEN should show fakes generated`() {
        // GIVEN
        val summary = createTestSummary(totalTimeMs = 1238)

        // WHEN
        val message = CompilationReport.successMessage(summary, LogLevel.INFO)

        // THEN
        assertTrue(message.contains("✅"))
        assertTrue(message.contains("121 fakes generated"))
        assertTrue(message.contains("1.2s"))
    }

    @Test
    fun `GIVEN QUIET level or no fakes WHEN generating success message THEN should return empty`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val quietMessage = CompilationReport.successMessage(summary, LogLevel.QUIET)
        val noFakesMessage =
            CompilationReport.successMessage(
                createTestSummary(interfacesProcessed = 0, classesProcessed = 0),
                LogLevel.INFO,
            )

        // THEN
        assertEquals("", quietMessage, "QUIET should return empty")
        assertEquals("", noFakesMessage, "No fakes should return empty")
    }
}
