// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for UnifiedMetricsTree.toInfoSummary() - concise INFO-level output.
 *
 * Tests follow GIVEN-WHEN-THEN pattern and use vanilla JUnit5 + kotlin-test.
 */
class UnifiedMetricsTreeInfoSummaryTest {
    @Test
    fun `GIVEN single interface WHEN calling toInfoSummary THEN should return concise 4-line summary`() =
        runTest {
            // GIVEN: Tree with single interface metrics
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000, // 45µs
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 535_000, // 535µs
                                irLOC = 73,
                            ),
                        ),
                    classes = emptyList(),
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should be exactly 4 lines
            val lines = summary.split("\n")
            assertEquals(4, lines.size, "INFO summary should be exactly 4 lines")
            assertTrue(lines[0].contains("1 fakes generated"), "Line 1 should show total count")
            assertTrue(lines[1].contains("Interfaces: 1"), "Line 2 should show breakdown")
            assertTrue(
                lines[2].contains("FIR:") && lines[2].contains("IR:"),
                "Line 3 should show phase timing",
            )
            assertTrue(lines[3].contains("Cache:"), "Line 4 should show cache stats")
        }

    @Test
    fun `GIVEN multiple interfaces WHEN calling toInfoSummary THEN should aggregate totals`() =
        runTest {
            // GIVEN: Tree with multiple interface metrics
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 535_000,
                                irLOC = 73,
                            ),
                            UnifiedFakeMetrics(
                                name = "DataCache",
                                firTimeNanos = 40_000,
                                firTypeParamCount = 1,
                                firMemberCount = 3,
                                irTimeNanos = 300_000,
                                irLOC = 58,
                            ),
                        ),
                    classes = emptyList(),
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should aggregate totals
            assertContains(summary, "2 fakes generated")
            assertContains(summary, "Interfaces: 2")
            assertContains(summary, "Classes: 0")
        }

    @Test
    fun `GIVEN interfaces and classes WHEN calling toInfoSummary THEN should show both types`() =
        runTest {
            // GIVEN: Tree with both interfaces and classes
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 535_000,
                                irLOC = 73,
                            ),
                            UnifiedFakeMetrics(
                                name = "DataCache",
                                firTimeNanos = 40_000,
                                firTypeParamCount = 1,
                                firMemberCount = 3,
                                irTimeNanos = 300_000,
                                irLOC = 58,
                            ),
                        ),
                    classes =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "DataHolder",
                                firTimeNanos = 30_000,
                                firTypeParamCount = 1,
                                firMemberCount = 2,
                                irTimeNanos = 90_000,
                                irLOC = 45,
                            ),
                        ),
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should show both types
            assertContains(summary, "3 fakes generated")
            assertContains(summary, "Interfaces: 2")
            assertContains(summary, "Classes: 1")
        }

    @Test
    fun `GIVEN empty metrics WHEN calling toInfoSummary THEN should handle gracefully`() =
        runTest {
            // GIVEN: Empty tree
            val tree = UnifiedMetricsTree(interfaces = emptyList(), classes = emptyList())

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should handle zero fakes gracefully
            assertContains(summary, "0 fakes generated")
            assertContains(summary, "Interfaces: 0")
            assertContains(summary, "Classes: 0")
            assertContains(summary, "0/0 (0%)")
        }

    @Test
    fun `GIVEN fast IR times WHEN calling toInfoSummary THEN should estimate cache hits`() =
        runTest {
            // GIVEN: Tree with very fast IR times (< 100µs per fake = cached)
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "CachedInterface1",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 50_000, // 50µs - likely cached
                                irLOC = 73,
                            ),
                            UnifiedFakeMetrics(
                                name = "CachedInterface2",
                                firTimeNanos = 40_000,
                                firTypeParamCount = 1,
                                firMemberCount = 3,
                                irTimeNanos = 30_000, // 30µs - likely cached
                                irLOC = 58,
                            ),
                        ),
                    classes = emptyList(),
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should estimate cache hits based on fast IR times
            // Average IR time = (50_000 + 30_000) / 2 = 40_000ns < 100_000ns threshold
            assertContains(summary, "2 fakes generated")
            assertContains(summary, "2 cached")
            assertContains(summary, "2/2 (100%)")
        }

    @Test
    fun `GIVEN slow IR times WHEN calling toInfoSummary THEN should estimate zero cache hits`() =
        runTest {
            // GIVEN: Tree with slow IR times (>= 100µs per fake = fresh generation)
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "FreshInterface1",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 535_000, // 535µs - fresh generation
                                irLOC = 73,
                            ),
                            UnifiedFakeMetrics(
                                name = "FreshInterface2",
                                firTimeNanos = 40_000,
                                firTypeParamCount = 1,
                                firMemberCount = 3,
                                irTimeNanos = 300_000, // 300µs - fresh generation
                                irLOC = 58,
                            ),
                        ),
                    classes = emptyList(),
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should estimate zero cache hits
            // Average IR time = (535_000 + 300_000) / 2 = 417_500ns >= 100_000ns threshold
            assertContains(summary, "2 fakes generated")
            assertContains(summary, "0 cached")
            assertContains(summary, "0/2 (0%)")
        }

    @Test
    fun `GIVEN metrics WHEN calling toInfoSummary THEN should show time units`() =
        runTest {
            // GIVEN: Tree with metrics
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 115_000, // 115µs
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 1_285_000, // 1.285ms
                                irLOC = 73,
                            ),
                        ),
                    classes = emptyList(),
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should include time formatting (µs or ms)
            // Note: Exact format depends on TimeFormatter implementation
            assertTrue(
                summary.contains("µs") || summary.contains("ms"),
            )
            assertTrue(summary.contains("FIR:"))
            assertTrue(summary.contains("IR:"))
        }

    @Test
    fun `GIVEN summary format WHEN checking structure THEN should be 4 lines without trailing newline`() =
        runTest {
            // GIVEN: Tree with metrics
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 535_000,
                                irLOC = 73,
                            ),
                        ),
                    classes = emptyList(),
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should have correct structure
            val lines = summary.split("\n")
            assertEquals(4, lines.size, "Should have exactly 4 lines")

            // Verify each line has expected prefix/structure
            assertTrue(lines[0].startsWith("Fakt:"))
            assertTrue(lines[1].startsWith("  Interfaces:"))
            assertTrue(lines[2].startsWith("  FIR:"))
            assertTrue(lines[3].startsWith("  Cache:"))
        }

    @Test
    fun `GIVEN large fake count WHEN calling toInfoSummary THEN should handle double-digit counts`() =
        runTest {
            // GIVEN: Tree with many fakes
            val interfaces =
                List(42) { index ->
                    UnifiedFakeMetrics(
                        name = "Interface$index",
                        firTimeNanos = 45_000,
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 535_000,
                        irLOC = 73,
                    )
                }
            val classes =
                List(5) { index ->
                    UnifiedFakeMetrics(
                        name = "Class$index",
                        firTimeNanos = 30_000,
                        firTypeParamCount = 1,
                        firMemberCount = 2,
                        irTimeNanos = 90_000,
                        irLOC = 45,
                    )
                }
            val tree = UnifiedMetricsTree(interfaces = interfaces, classes = classes)

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should handle large counts correctly
            assertContains(summary, "47 fakes generated")
            assertContains(summary, "Interfaces: 42")
            assertContains(summary, "Classes: 5")
        }
}
