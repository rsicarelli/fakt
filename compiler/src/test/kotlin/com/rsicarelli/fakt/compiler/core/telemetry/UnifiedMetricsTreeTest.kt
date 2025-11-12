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
 */
class UnifiedMetricsTreeTest {

    @Test
    fun `GIVEN single interface WHEN converting to tree THEN should format with proper branching`() =
        runTest {
            // GIVEN: Single interface metrics
            val interfaceMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "UserService",
                    firTimeNanos = 45_000, // 45µs
                    firTypeParamCount = 0,
                    firMemberCount = 5,
                    irTimeNanos = 535_000, // 535µs
                    irLOC = 73
                )
            )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should format with proper structure
            assertContains(result, "FIR + IR trace")
            assertContains(result, "├─ Total FIR time")
            assertContains(result, "├─ Total IR time")
            assertContains(result, "├─ Total time")
            assertContains(result, "├─ Interfaces: 1")
            assertContains(result, "│  └─ UserService") // Last interface closes branch
            assertContains(result, "├─ FIR analysis: 0 type parameters, 5 members")
            assertContains(result, "└─ IR generation: FakeUserServiceImpl 73 LOC")
        }

    @Test
    fun `GIVEN multiple interfaces WHEN converting to tree THEN should show all with correct branching`() =
        runTest {
            // GIVEN: Multiple interface metrics
            val interfaceMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "UserService",
                    firTimeNanos = 45_000,
                    firTypeParamCount = 0,
                    firMemberCount = 5,
                    irTimeNanos = 535_000,
                    irLOC = 73
                ),
                UnifiedFakeMetrics(
                    name = "DataCache",
                    firTimeNanos = 40_000,
                    firTypeParamCount = 1,
                    firMemberCount = 3,
                    irTimeNanos = 300_000,
                    irLOC = 58
                )
            )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show both interfaces with proper branching
            assertContains(result, "├─ Interfaces: 2")
            assertContains(result, "│  ├─ UserService") // First interface uses ├─
            assertContains(result, "│  └─ DataCache") // Last interface uses └─
            assertContains(result, "FakeUserServiceImpl 73 LOC")
            assertContains(result, "FakeDataCacheImpl 58 LOC")
            // Verify unique FIR lines (the bug we're fixing)
            assertContains(result, "0 type parameters, 5 members")
            assertContains(result, "1 type parameters, 3 members")
        }

    @Test
    fun `GIVEN interfaces and classes WHEN converting to tree THEN should show both sections`() =
        runTest {
            // GIVEN: Both interface and class metrics
            val interfaceMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "UserService",
                    firTimeNanos = 45_000,
                    firTypeParamCount = 0,
                    firMemberCount = 5,
                    irTimeNanos = 535_000,
                    irLOC = 73
                )
            )
            val classMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "DataHolder",
                    firTimeNanos = 30_000,
                    firTypeParamCount = 1,
                    firMemberCount = 2,
                    irTimeNanos = 90_000,
                    irLOC = 45
                )
            )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = classMetrics)

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show both sections with correct structure
            assertContains(result, "├─ Interfaces: 1")
            assertContains(result, "│  ├─ UserService") // Interface doesn't close (classes follow)
            assertContains(result, "└─ Classes: 1") // Classes section closes the tree
            assertContains(result, "   └─ DataHolder") // Class uses different prefix
            assertContains(result, "FakeUserServiceImpl")
            assertContains(result, "FakeDataHolderImpl")
        }

    @Test
    fun `GIVEN multiple classes WHEN converting to tree THEN should show correct branching`() =
        runTest {
            // GIVEN: Multiple class metrics
            val classMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "DataHolder",
                    firTimeNanos = 30_000,
                    firTypeParamCount = 1,
                    firMemberCount = 2,
                    irTimeNanos = 90_000,
                    irLOC = 45
                ),
                UnifiedFakeMetrics(
                    name = "ConfigWrapper",
                    firTimeNanos = 25_000,
                    firTypeParamCount = 0,
                    firMemberCount = 3,
                    irTimeNanos = 80_000,
                    irLOC = 38
                )
            )
            val tree = UnifiedMetricsTree(interfaces = emptyList(), classes = classMetrics)

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show classes with proper branching
            assertContains(result, "└─ Classes: 2")
            assertContains(result, "   ├─ DataHolder") // First class uses ├─
            assertContains(result, "   └─ ConfigWrapper") // Last class uses └─
        }

    @Test
    fun `GIVEN empty metrics WHEN converting to tree THEN should show zero counts`() =
        runTest {
            // GIVEN: Empty metrics
            val tree = UnifiedMetricsTree(interfaces = emptyList(), classes = emptyList())

            // WHEN: Converting to tree string
            val result = tree.toTreeString()

            // THEN: Should show header with zero counts
            assertContains(result, "FIR + IR trace")
            assertContains(result, "├─ Interfaces: 0")
            assertContains(result, "0µs") // Zero time
        }

    @Test
    fun `GIVEN metrics WHEN computing totals THEN should sum correctly`() =
        runTest {
            // GIVEN: Interface and class metrics
            val interfaceMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "UserService",
                    firTimeNanos = 45_000,
                    firTypeParamCount = 0,
                    firMemberCount = 5,
                    irTimeNanos = 535_000,
                    irLOC = 73
                )
            )
            val classMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "DataHolder",
                    firTimeNanos = 30_000,
                    firTypeParamCount = 1,
                    firMemberCount = 2,
                    irTimeNanos = 90_000,
                    irLOC = 45
                )
            )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = classMetrics)

            // WHEN: Computing totals
            val totalFir = tree.totalFirTimeNanos
            val totalIr = tree.totalIrTimeNanos
            val totalTime = tree.totalTimeNanos

            // THEN: Should sum correctly
            assertEquals(75_000, totalFir, "FIR time = 45µs + 30µs = 75µs")
            assertEquals(625_000, totalIr, "IR time = 535µs + 90µs = 625µs")
            assertEquals(700_000, totalTime, "Total = 75µs + 625µs = 700µs")
        }

    @Test
    fun `GIVEN custom target column WHEN formatting THEN should right-align at specified column`() =
        runTest {
            // GIVEN: Simple metrics
            val interfaceMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "Short",
                    firTimeNanos = 10_000,
                    firTypeParamCount = 0,
                    firMemberCount = 1,
                    irTimeNanos = 50_000,
                    irLOC = 10
                )
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
            val interfaceMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "VeryLongInterfaceNameThatWillDefinitelyOverflowTheTargetColumnWidth",
                    firTimeNanos = 45_000,
                    firTypeParamCount = 0,
                    firMemberCount = 5,
                    irTimeNanos = 535_000,
                    irLOC = 73
                )
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
            val interfaceMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "FastInterface",
                    firTimeNanos = 500, // Microseconds
                    firTypeParamCount = 0,
                    firMemberCount = 1,
                    irTimeNanos = 800, // Microseconds
                    irLOC = 10
                ),
                UnifiedFakeMetrics(
                    name = "SlowInterface",
                    firTimeNanos = 5_000_000, // Milliseconds
                    firTypeParamCount = 0,
                    firMemberCount = 5,
                    irTimeNanos = 10_000_000, // Milliseconds
                    irLOC = 100
                )
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
            // GIVEN: Multiple interfaces with similar FIR analysis lines
            val interfaceMetrics = listOf(
                UnifiedFakeMetrics(
                    name = "Interface1",
                    firTimeNanos = 45_000,
                    firTypeParamCount = 0,
                    firMemberCount = 5,
                    irTimeNanos = 535_000,
                    irLOC = 73
                ),
                UnifiedFakeMetrics(
                    name = "Interface2",
                    firTimeNanos = 45_000,
                    firTypeParamCount = 0,
                    firMemberCount = 5,
                    irTimeNanos = 535_000,
                    irLOC = 73
                )
            )
            val tree = UnifiedMetricsTree(interfaces = interfaceMetrics, classes = emptyList())

            // WHEN: Converting to tree (single string output)
            val result = tree.toTreeString()
            val lines = result.lines()

            // THEN: All lines should be present (no filtering)
            // Both interfaces should be visible (check for branch patterns to avoid matching FakeXxxImpl lines)
            assertEquals(1, lines.count { it.contains("├─ Interface1") || it.contains("└─ Interface1") },
                "Interface1 should appear once in tree structure")
            assertEquals(1, lines.count { it.contains("├─ Interface2") || it.contains("└─ Interface2") },
                "Interface2 should appear once in tree structure")

            // Both FIR analysis lines should be present (this was the bug!)
            assertEquals(2, lines.count { it.contains("FIR analysis: 0 type parameters, 5 members") },
                "Both FIR analysis lines should be present")
        }
}
