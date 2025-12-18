// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for UnifiedMetricsTree.toInfoSummary() - concise INFO-level output.
 *
 * New format is a single line based on cache state:
 * - All cached: "Fakt: X fakes (all cached)"
 * - Some regenerated: "Fakt: X fakes in Y (N regenerated, M cached)"
 * - None cached: "Fakt: X fakes generated in Y"
 *
 * Tests follow GIVEN-WHEN-THEN pattern and use vanilla JUnit5 + kotlin-test.
 */
class UnifiedMetricsTreeInfoSummaryTest {
    @Test
    fun `GIVEN all fakes cached WHEN calling toInfoSummary THEN should return minimal cached message`() =
        runTest {
            // GIVEN: Tree with 2 fakes, all cached
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 50_000, // Fast - cached
                                irLOC = 73,
                            ),
                            UnifiedFakeMetrics(
                                name = "DataCache",
                                firTimeNanos = 40_000,
                                firTypeParamCount = 1,
                                firMemberCount = 3,
                                irTimeNanos = 30_000, // Fast - cached
                                irLOC = 58,
                            ),
                        ),
                    classes = emptyList(),
                    irCacheHits = 2, // All 2 fakes cached
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should show minimal cached message
            assertEquals("Fakt: 2 fakes (all cached)", summary)
        }

    @Test
    fun `GIVEN some fakes regenerated WHEN calling toInfoSummary THEN should show breakdown`() =
        runTest {
            // GIVEN: Tree with 3 fakes, 2 cached and 1 regenerated
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 535_000, // Slow - regenerated
                                irLOC = 73,
                            ),
                            UnifiedFakeMetrics(
                                name = "DataCache",
                                firTimeNanos = 40_000,
                                firTypeParamCount = 1,
                                firMemberCount = 3,
                                irTimeNanos = 30_000, // Fast - cached
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
                                irTimeNanos = 20_000, // Fast - cached
                                irLOC = 45,
                            ),
                        ),
                    irCacheHits = 2, // 2 of 3 cached
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should show breakdown with regenerated and cached counts
            assertContains(summary, "Fakt: 3 fakes in")
            assertContains(summary, "1 regenerated")
            assertContains(summary, "2 cached")
        }

    @Test
    fun `GIVEN no fakes cached WHEN calling toInfoSummary THEN should show generation message`() =
        runTest {
            // GIVEN: Tree with 2 fakes, none cached
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
                    irCacheHits = 0, // None cached
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should show simple generation message
            assertContains(summary, "Fakt: 2 fakes generated in")
            assertFalse(summary.contains("cached"), "Should not mention cache when none cached")
        }

    @Test
    fun `GIVEN empty metrics WHEN calling toInfoSummary THEN should show generation message`() =
        runTest {
            // GIVEN: Empty tree
            val tree = UnifiedMetricsTree(interfaces = emptyList(), classes = emptyList())

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should handle zero fakes gracefully
            assertContains(summary, "Fakt: 0 fakes generated in")
        }

    @Test
    fun `GIVEN single interface cached WHEN calling toInfoSummary THEN should show all cached`() =
        runTest {
            // GIVEN: Tree with single cached interface
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 50_000,
                                irLOC = 73,
                            ),
                        ),
                    classes = emptyList(),
                    irCacheHits = 1,
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should show minimal cached message
            assertEquals("Fakt: 1 fakes (all cached)", summary)
        }

    @Test
    fun `GIVEN time formatting WHEN calling toInfoSummary with regeneration THEN should include time units`() =
        runTest {
            // GIVEN: Tree with metrics (none cached to force time display)
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 115_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 1_285_000,
                                irLOC = 73,
                            ),
                        ),
                    classes = emptyList(),
                    irCacheHits = 0,
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should include time formatting (µs or ms)
            assertTrue(
                summary.contains("µs") || summary.contains("ms"),
                "Should include time unit",
            )
        }

    @Test
    fun `GIVEN summary format WHEN checking structure THEN should be single line`() =
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
                    irCacheHits = 0,
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should be single line (no newlines)
            assertFalse(summary.contains("\n"), "Summary should be single line")
            assertTrue(summary.startsWith("Fakt:"), "Summary should start with 'Fakt:'")
        }

    @Test
    fun `GIVEN large fake count WHEN all cached THEN should show all cached message`() =
        runTest {
            // GIVEN: Tree with many fakes, all cached
            val interfaces =
                List(42) { index ->
                    UnifiedFakeMetrics(
                        name = "Interface$index",
                        firTimeNanos = 45_000,
                        firTypeParamCount = 0,
                        firMemberCount = 5,
                        irTimeNanos = 50_000,
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
                        irTimeNanos = 20_000,
                        irLOC = 45,
                    )
                }
            val tree =
                UnifiedMetricsTree(
                    interfaces = interfaces,
                    classes = classes,
                    irCacheHits = 47, // All 47 cached
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should show all cached message
            assertEquals("Fakt: 47 fakes (all cached)", summary)
        }

    @Test
    fun `GIVEN large fake count WHEN none cached THEN should show generation message`() =
        runTest {
            // GIVEN: Tree with many fakes, none cached
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
            val tree =
                UnifiedMetricsTree(
                    interfaces = interfaces,
                    classes = classes,
                    irCacheHits = 0,
                )

            // WHEN: Formatting as INFO summary
            val summary = tree.toInfoSummary()

            // THEN: Should show generation message with count
            assertContains(summary, "Fakt: 47 fakes generated in")
        }

    @Test
    fun `GIVEN allIrCached property WHEN all cached THEN should return true`() =
        runTest {
            // GIVEN: Tree with all fakes cached
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 50_000,
                                irLOC = 73,
                            ),
                        ),
                    classes = emptyList(),
                    irCacheHits = 1,
                )

            // THEN: allIrCached should be true
            assertTrue(tree.allIrCached)
        }

    @Test
    fun `GIVEN allIrCached property WHEN not all cached THEN should return false`() =
        runTest {
            // GIVEN: Tree with partial cache
            val tree =
                UnifiedMetricsTree(
                    interfaces =
                        listOf(
                            UnifiedFakeMetrics(
                                name = "UserService",
                                firTimeNanos = 45_000,
                                firTypeParamCount = 0,
                                firMemberCount = 5,
                                irTimeNanos = 50_000,
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
                    irCacheHits = 1, // Only 1 of 2 cached
                )

            // THEN: allIrCached should be false
            assertFalse(tree.allIrCached)
        }

    @Test
    fun `GIVEN allIrCached property WHEN empty THEN should return false`() =
        runTest {
            // GIVEN: Empty tree
            val tree = UnifiedMetricsTree(interfaces = emptyList(), classes = emptyList())

            // THEN: allIrCached should be false (no fakes to cache)
            assertFalse(tree.allIrCached)
        }
}
