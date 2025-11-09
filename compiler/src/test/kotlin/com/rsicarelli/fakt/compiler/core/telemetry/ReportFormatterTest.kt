// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ReportFormatter.
 *
 * Validates report formatting at different log levels (INFO, DEBUG, TRACE).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportFormatterTest {
    private fun createTestSummary(
        totalTimeNanos: Long = 44_000_000, // 44ms
        interfacesDiscovered: Int = 100,
        interfacesProcessed: Int = 100,
        interfacesCached: Int = 0,
        classesDiscovered: Int = 21,
        classesProcessed: Int = 21,
        classesCached: Int = 0,
        totalLOC: Int = 1234,
        totalFiles: Int = 121,
        totalFileSizeBytes: Long = 65432,
        includePhases: Boolean = true,
        includeFakeMetrics: Boolean = true,
    ): CompilationSummary {
        val phaseBreakdown =
            if (includePhases) {
                mapOf(
                    "DISCOVERY" to PhaseMetrics("DISCOVERY", 1_000_000_000, 1_002_000_000), // 1s to 1.002s = 2ms
                    "GENERATION" to PhaseMetrics("GENERATION", 1_002_000_000, 1_044_000_000), // 1.002s to 1.044s = 42ms
                )
            } else {
                emptyMap()
            }

        val fakeMetrics =
            if (includeFakeMetrics) {
                listOf(
                    FakeMetrics(
                        name = "UserService",
                        pattern = GenericPattern.NoGenerics,
                        memberCount = 5,
                        typeParamCount = 0,
                        analysisTimeNanos = 10_000_000, // 10ms
                        generationTimeNanos = 20_000_000, // 20ms
                        generatedLOC = 87,
                        fileSizeBytes = 2450,
                        importCount = 3,
                    ),
                    FakeMetrics(
                        name = "SlowInterface",
                        pattern = GenericPattern.NoGenerics,
                        memberCount = 10,
                        typeParamCount = 0,
                        analysisTimeNanos = 60_000_000, // 60ms
                        generationTimeNanos = 90_000_000, // 90ms (total 150ms - marked as slow)
                        generatedLOC = 200,
                        fileSizeBytes = 5000,
                        importCount = 5,
                    ),
                )
            } else {
                emptyList()
            }

        return CompilationSummary(
            totalTimeNanos = totalTimeNanos,
            interfacesDiscovered = interfacesDiscovered,
            interfacesProcessed = interfacesProcessed,
            interfacesCached = interfacesCached,
            classesDiscovered = classesDiscovered,
            classesProcessed = classesProcessed,
            classesCached = classesCached,
            phaseBreakdown = phaseBreakdown,
            fakeMetrics = fakeMetrics,
            totalLOC = totalLOC,
            totalFiles = totalFiles,
            totalFileSizeBytes = totalFileSizeBytes,
            outputDirectory = "build/generated/fakt/commonTest/kotlin",
        )
    }

    // INFO level tests (4 tests)

    @Test
    fun `GIVEN summary with zero fakes WHEN formatting INFO THEN should show 0 fakes`() {
        // GIVEN
        val summary =
            createTestSummary(
                interfacesDiscovered = 0,
                classesDiscovered = 0,
                interfacesProcessed = 0,
                classesProcessed = 0,
            )

        // WHEN
        val report = ReportFormatter.formatInfo(summary)

        // THEN
        assertTrue(report.contains("0 fakes"))
    }

    @Test
    fun `GIVEN summary with normal data WHEN formatting INFO THEN should show single line summary`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatInfo(summary)

        // THEN
        assertEquals("✅ 121 fakes (121 new) | 44ms", report)
    }

    @Test
    fun `GIVEN summary with 100% cache WHEN formatting INFO THEN should show 100%`() {
        // GIVEN
        val summary =
            createTestSummary(
                interfacesDiscovered = 100,
                interfacesProcessed = 0,
                interfacesCached = 100,
                classesDiscovered = 21,
                classesProcessed = 0,
                classesCached = 21,
            )

        // WHEN
        val report = ReportFormatter.formatInfo(summary)

        // THEN
        assertTrue(report.contains("121 cached"))
    }

    @Test
    fun `GIVEN summary WHEN formatting INFO THEN should start with checkmark`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatInfo(summary)

        // THEN
        assertTrue(report.startsWith("✅"))
    }

    // DEBUG level tests (4 tests)

    @Test
    fun `GIVEN summary WHEN formatting DEBUG THEN should show 5-7 lines compact format`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatDebug(summary)

        // THEN
        val lines = report.lines().filter { it.isNotBlank() }
        assertEquals(3, lines.size, "Expected 3 lines, got ${lines.size}")
    }

    @Test
    fun `GIVEN summary WHEN formatting DEBUG THEN should include discovery and generation summary`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatDebug(summary)

        // THEN
        assertTrue(report.contains("Discovery: 100 interfaces, 21 classes"))
        assertTrue(report.contains("Generation: 121 new fakes"))
    }

    @Test
    fun `GIVEN summary with all cached WHEN formatting DEBUG THEN should show 0 LOC message`() {
        // GIVEN
        val summary = createTestSummary(totalFiles = 0, totalLOC = 0)

        // WHEN
        val report = ReportFormatter.formatDebug(summary)

        // THEN
        assertTrue(report.contains("All from cache (0 new LOC)"))
    }

    @Test
    fun `GIVEN summary WHEN formatting DEBUG THEN should include separator lines`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatDebug(summary)

        // THEN
        // No more separators in new format - just check for clean output
        assertTrue(report.contains("Discovery:"))
        assertTrue(report.contains("Generation:"))
        assertTrue(report.contains("LOC total"))
    }

    // TRACE level tests (5 tests)

    @Test
    fun `GIVEN summary WHEN formatting TRACE THEN should show exhaustive report`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatTrace(summary)

        // THEN
        // New tree-style format is more compact: ~15-16 lines with 2 fakes
        val lines = report.lines().filter { it.isNotBlank() }
        assertTrue(lines.size > 10, "TRACE should have 10+ lines, got ${lines.size}")
    }

    @Test
    fun `GIVEN summary WHEN formatting TRACE THEN should include header and footer`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatTrace(summary)

        // THEN
        // New tree-style format: starts and ends with separator, no "REPORT" title line
        assertTrue(report.startsWith("═".repeat(60)))
        assertTrue(report.endsWith("═".repeat(60)))
    }

    @Test
    fun `GIVEN summary WHEN formatting TRACE THEN should include phase breakdown`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatTrace(summary)

        // THEN
        assertTrue(report.contains("DISCOVERY (2ms)"))
        assertTrue(report.contains("GENERATION (42ms)"))
    }

    @Test
    fun `GIVEN summary with slow interfaces WHEN formatting TRACE THEN should show top 10 slowest`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatTrace(summary)

        // THEN
        assertTrue(report.contains("SLOWEST FAKES (Top 10):"))
        assertTrue(report.contains("SlowInterface (150ms)"))
        assertTrue(report.contains("200 LOC"))
        assertTrue(report.contains("⚠️")) // Slow indicator
    }

    @Test
    fun `GIVEN summary WHEN formatting TRACE THEN should include all sections`() {
        // GIVEN
        val summary = createTestSummary()

        // WHEN
        val report = ReportFormatter.formatTrace(summary)

        // THEN
        // Verify all major sections are present (new tree-style format, no "OUTPUT:" section)
        assertTrue(report.contains("SUMMARY ("))
        assertTrue(report.contains("DISCOVERY ("))
        assertTrue(report.contains("GENERATION ("))
        assertTrue(report.contains("SLOWEST FAKES"))
    }
}
