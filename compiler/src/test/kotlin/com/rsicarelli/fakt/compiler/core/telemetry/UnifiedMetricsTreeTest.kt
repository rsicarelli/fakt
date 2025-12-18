// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for UnifiedMetricsTree - tree-formatted metrics aggregation.
 *
 * Tests follow GIVEN-WHEN-THEN pattern and use vanilla JUnit5 + kotlin-test.
 *
 * **New Format (v2):**
 * - Only regenerated/generated fakes appear in tree (cached fakes are hidden)
 * - Single "Generated:" or "Regenerated:" section (no separate Interfaces/Classes)
 * - When all cached: just show INFO summary
 */
class UnifiedMetricsTreeTest {
    @Test
    fun `GIVEN single regenerated interface WHEN converting to tree THEN should format with proper branching`() =
        runTest {
            // GIVEN: Single interface metrics (represents regenerated fake)
            val interfaceMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "UserService",
                        firTimeNanos = 45_000, // 45µs
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 535_000, // 535µs
                        irLOC = 73,
                    ),
                )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should format with proper structure
            assertContains(result, "Fakt Trace")
            assertContains(result, "   ├─ FIR→IR cache transformation")
            assertContains(result, "   ├─ FIR Time")
            assertContains(result, "   ├─ IR Time")
            assertContains(result, "   ├─ Total time")
            assertContains(result, "   ├─ Stats")
            assertContains(result, "   │  ├─ Total fakes")
            assertContains(result, "   │  ├─ Avg Time per Fake")
            assertContains(result, "Cache hit rate")
            assertContains(result, "   └─ Generated: 1")
            assertContains(result, "UserService")
            assertContains(result, "└─ FakeUserServiceImpl")
            assertContains(result, "73 LOC")
        }

    @Test
    fun `GIVEN multiple regenerated fakes WHEN converting to tree THEN should show all with correct branching`() =
        runTest {
            // GIVEN: Multiple interface metrics (regenerated fakes)
            val interfaceMetrics =
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
                )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show all regenerated fakes in single section
            assertContains(result, "   └─ Generated: 2") // Fresh build = "Generated"
            assertContains(result, "UserService")
            assertContains(result, "DataCache")
            assertContains(result, "FakeUserServiceImpl")
            assertContains(result, "73 LOC")
            assertContains(result, "FakeDataCacheImpl")
            assertContains(result, "58 LOC")
        }

    @Test
    fun `GIVEN interfaces and classes WHEN converting to tree THEN should combine in single section`() =
        runTest {
            // GIVEN: Both interface and class metrics (combined in single section)
            val interfaceMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "UserService",
                        firTimeNanos = 45_000,
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 535_000,
                        irLOC = 73,
                    ),
                )
            val classMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "DataHolder",
                        firTimeNanos = 30_000,
                        firTypeParamCount = 1,
                        firMemberCount = 2,
                        irTimeNanos = 90_000,
                        irLOC = 45,
                    ),
                )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = classMetrics)

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show both in single "Generated" section
            assertContains(result, "   └─ Generated: 2") // Combined count
            assertContains(result, "UserService")
            assertContains(result, "DataHolder")
            assertContains(result, "FakeUserServiceImpl")
            assertContains(result, "73 LOC")
            assertContains(result, "FakeDataHolderImpl")
            assertContains(result, "45 LOC")
        }

    @Test
    fun `GIVEN multiple classes WHEN converting to tree THEN should show correct branching`() =
        runTest {
            // GIVEN: Multiple class metrics
            val classMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "DataHolder",
                        firTimeNanos = 30_000,
                        firTypeParamCount = 1,
                        firMemberCount = 2,
                        irTimeNanos = 90_000,
                        irLOC = 45,
                    ),
                    UnifiedFakeMetrics(
                        name = "ConfigWrapper",
                        firTimeNanos = 25_000,
                        firTypeParamCount = 0,
                        firMemberCount = 3,
                        irTimeNanos = 80_000,
                        irLOC = 38,
                    ),
                )
            val tree = UnifiedMetricsTree(interfaces = emptyList(), classes = classMetrics)

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show classes in Generated section
            assertContains(result, "   └─ Generated: 2")
            assertContains(result, "DataHolder")
            assertContains(result, "ConfigWrapper")
        }

    @Test
    fun `GIVEN empty metrics WHEN converting to tree THEN should show zero counts`() =
        runTest {
            // GIVEN: Empty metrics (all cached scenario)
            val tree = UnifiedMetricsTree(interfaces = emptyList(), classes = emptyList())

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show header with zero counts
            assertContains(result, "Fakt Trace")
            assertContains(result, "   ├─ FIR→IR cache transformation")
            assertContains(result, "   ├─ Stats")
            assertContains(result, "Total fakes")
            assertContains(result, "0µs") // Zero time
        }

    @Test
    fun `GIVEN metrics WHEN computing totals THEN should sum correctly`() =
        runTest {
            // GIVEN: Interface and class metrics with transformation time
            val interfaceMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "UserService",
                        firTimeNanos = 45_000,
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 535_000,
                        irLOC = 73,
                    ),
                )
            val classMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "DataHolder",
                        firTimeNanos = 30_000,
                        firTypeParamCount = 1,
                        firMemberCount = 2,
                        irTimeNanos = 90_000,
                        irLOC = 45,
                    ),
                )
            val transformationTimeNanos = 100_000L // 100µs
            val tree = UnifiedMetricsTree(
                interfaces = interfaceMetrics,
                classes = classMetrics,
                transformationTimeNanos = transformationTimeNanos,
            )

            // WHEN: Computing totals
            val totalFir = tree.totalFirTimeNanos
            val totalIr = tree.totalIrTimeNanos
            val totalTime = tree.totalTimeNanos

            // THEN: Should sum correctly (including transformation time)
            assertEquals(75_000, totalFir, "FIR time = 45µs + 30µs = 75µs")
            assertEquals(625_000, totalIr, "IR time = 535µs + 90µs = 625µs")
            assertEquals(800_000, totalTime, "Total = 75µs + 625µs + 100µs = 800µs")
        }

    @Test
    fun `GIVEN custom target column WHEN formatting THEN should right-align at specified column`() =
        runTest {
            // GIVEN: Simple metrics
            val interfaceMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "Short",
                        firTimeNanos = 10_000,
                        firTypeParamCount = 0,
                        firMemberCount = 1,
                        irTimeNanos = 50_000,
                        irLOC = 10,
                    ),
                )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Formatting with custom target column
            val result80 = tree.toTreeString(targetColumn = 80)
            val result100 = tree.toTreeString(targetColumn = 100)

            // THEN: Different columns should produce different padding
            assertTrue(result80.isNotBlank(), "Should produce output with column 80")
            assertTrue(result100.isNotBlank(), "Should produce output with column 100")
            assertTrue(result100.length > result80.length, "Column 100 should have more padding")
        }

    @Test
    fun `GIVEN long interface names WHEN formatting THEN should handle overflow gracefully`() =
        runTest {
            // GIVEN: Interface with very long name
            val interfaceMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "VeryLongInterfaceNameThatWillDefinitelyOverflowTheTargetColumnWidth",
                        firTimeNanos = 45_000,
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 535_000,
                        irLOC = 73,
                    ),
                )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should handle overflow without crashing
            assertContains(result, "VeryLongInterfaceNameThatWillDefinitelyOverflowTheTargetColumnWidth")
            assertContains(result, "µs") // Time should still appear
        }

    @Test
    fun `GIVEN various time values WHEN formatting THEN should use appropriate units`() =
        runTest {
            // GIVEN: Metrics with different time scales
            val interfaceMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "FastInterface",
                        firTimeNanos = 500, // Microseconds
                        firTypeParamCount = 0,
                        firMemberCount = 1,
                        irTimeNanos = 800, // Microseconds
                        irLOC = 10,
                    ),
                    UnifiedFakeMetrics(
                        name = "SlowInterface",
                        firTimeNanos = 5_000_000, // Milliseconds
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 10_000_000, // Milliseconds
                        irLOC = 100,
                    ),
                )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show appropriate time units
            // Note: Exact formatting depends on TimeFormatter implementation
            assertContains(result, "µs") // Microseconds for fast operations
            assertContains(result, "ms") // Milliseconds for slow operations
        }

    @Test
    fun `GIVEN metrics tree WHEN each line is unique THEN should avoid Gradle filtering`() =
        runTest {
            // GIVEN: Multiple interfaces with similar metrics
            val interfaceMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "Interface1",
                        firTimeNanos = 45_000,
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 535_000,
                        irLOC = 73,
                    ),
                    UnifiedFakeMetrics(
                        name = "Interface2",
                        firTimeNanos = 45_000,
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 535_000,
                        irLOC = 73,
                    ),
                )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Converting to tree (single string output)
            val result = tree.toTreeString()
            val lines = result.lines()

            // THEN: All fakes should be present
            assertEquals(
                1,
                lines.count { it.contains("Interface1") && !it.contains("FakeInterface1Impl") },
                "Interface1 should appear once in tree structure",
            )
            assertEquals(
                1,
                lines.count { it.contains("Interface2") && !it.contains("FakeInterface2Impl") },
                "Interface2 should appear once in tree structure",
            )

            // Both fake impl lines should be present
            assertEquals(
                1,
                lines.count { it.contains("FakeInterface1Impl") },
                "FakeInterface1Impl should appear once",
            )
            assertEquals(
                1,
                lines.count { it.contains("FakeInterface2Impl") },
                "FakeInterface2Impl should appear once",
            )
        }

    @Test
    fun `GIVEN partial cache WHEN converting to tree THEN should show Generated label`() =
        runTest {
            // GIVEN: Some fakes generated with cache hits (others cached)
            val interfaceMetrics =
                listOf(
                    UnifiedFakeMetrics(
                        name = "ChangedService",
                        firTimeNanos = 45_000,
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 535_000,
                        irLOC = 73,
                    ),
                )
            val tree = UnifiedMetricsTree(
                interfaces = interfaceMetrics,
                classes = emptyList(),
                interfaceCount = 10, // Total interfaces
                classCount = 2, // Total classes
                irCacheHits = 11, // 11 of 12 from cache
            )

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should use "Generated" label (covers both new and regenerated)
            assertContains(result, "   └─ Generated: 1")
            assertContains(result, "ChangedService")
        }

    @Test
    fun `GIVEN all cached WHEN converting to info summary THEN should show concise message`() =
        runTest {
            // GIVEN: All fakes cached (empty metrics lists)
            val tree = UnifiedMetricsTree(
                interfaces = emptyList(),
                classes = emptyList(),
                interfaceCount = 100,
                classCount = 22,
                irCacheHits = 122,
            )

            // WHEN: Converting to info summary
            val result = tree.toInfoSummary()

            // THEN: Should show all cached message
            assertContains(result, "Fakt: 122 fakes (all cached)")
        }
}
