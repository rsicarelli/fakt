// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for PhaseTracker - compilation phase timing.
 *
 * Tests follow GIVEN-WHEN-THEN pattern and use vanilla JUnit5 + kotlin-test.
 */
class PhaseTrackerTest {
    @Test
    fun `GIVEN tracker WHEN starting a phase THEN should return phase ID`() =
        runTest {
            // GIVEN: Fresh phase tracker
            val tracker = PhaseTracker()

            // WHEN: Starting a phase
            val phaseId = tracker.startPhase("DISCOVERY")

            // THEN: Should return a non-null phase ID
            assertNotNull(phaseId, "Phase ID should not be null")
            assertTrue(phaseId.isNotEmpty(), "Phase ID should not be empty")
        }

    @Test
    fun `GIVEN started phase WHEN ending phase THEN should return metrics with duration`() =
        runTest {
            // GIVEN: Started phase
            val tracker = PhaseTracker()
            val phaseId = tracker.startPhase("ANALYSIS")

            // Simulate work
            Thread.sleep(10)

            // WHEN: Ending the phase
            val metrics = tracker.endPhase(phaseId)

            // THEN: Should return metrics with measurable duration
            assertEquals("ANALYSIS", metrics.name)
            assertTrue(metrics.duration >= 10, "Duration should be at least 10ms")
            assertEquals(0, metrics.subPhases.size, "Should have no sub-phases")
        }

    @Test
    fun `GIVEN nested phases WHEN ending phases THEN should track sub-phase hierarchy`() =
        runTest {
            // GIVEN: Nested phase structure
            val tracker = PhaseTracker()
            val parentId = tracker.startPhase("ANALYSIS")

            val subPhase1Id = tracker.startPhase("InterfaceA", parent = parentId)
            Thread.sleep(5)
            tracker.endPhase(subPhase1Id)

            val subPhase2Id = tracker.startPhase("InterfaceB", parent = parentId)
            Thread.sleep(5)
            tracker.endPhase(subPhase2Id)

            // WHEN: Ending parent phase
            val parentMetrics = tracker.endPhase(parentId)

            // THEN: Parent should contain both sub-phases
            assertEquals("ANALYSIS", parentMetrics.name)
            assertEquals(2, parentMetrics.subPhases.size, "Should have 2 sub-phases")
            assertEquals("InterfaceA", parentMetrics.subPhases[0].name)
            assertEquals("InterfaceB", parentMetrics.subPhases[1].name)
            assertTrue(
                parentMetrics.duration >=
                    parentMetrics.subPhases[0].duration +
                    parentMetrics.subPhases[1].duration,
                "Parent duration should be >= sum of sub-phase durations",
            )
        }

    @Test
    fun `GIVEN active phase WHEN checking is active THEN should return true`() =
        runTest {
            // GIVEN: Active phase
            val tracker = PhaseTracker()
            val phaseId = tracker.startPhase("GENERATION")

            // WHEN: Checking if phase is active
            val isActive = tracker.isPhaseActive(phaseId)

            // THEN: Should return true
            assertTrue(isActive, "Phase should be active")
        }

    @Test
    fun `GIVEN ended phase WHEN checking is active THEN should return false`() =
        runTest {
            // GIVEN: Phase that has ended
            val tracker = PhaseTracker()
            val phaseId = tracker.startPhase("GENERATION")
            tracker.endPhase(phaseId)

            // WHEN: Checking if phase is active
            val isActive = tracker.isPhaseActive(phaseId)

            // THEN: Should return false
            assertFalse(isActive, "Ended phase should not be active")
        }

    @Test
    fun `GIVEN multiple phases WHEN getting active count THEN should return correct count`() =
        runTest {
            // GIVEN: Multiple active phases
            val tracker = PhaseTracker()
            tracker.startPhase("PHASE1")
            tracker.startPhase("PHASE2")
            tracker.startPhase("PHASE3")

            // WHEN: Getting active count
            val count = tracker.activePhaseCount()

            // THEN: Should return 3
            assertEquals(3, count, "Should have 3 active phases")
        }

    @Test
    fun `GIVEN completed phases WHEN getting all completed THEN should return all metrics`() =
        runTest {
            // GIVEN: Multiple completed phases
            val tracker = PhaseTracker()
            val phase1Id = tracker.startPhase("DISCOVERY")
            tracker.endPhase(phase1Id)

            val phase2Id = tracker.startPhase("ANALYSIS")
            tracker.endPhase(phase2Id)

            // WHEN: Getting all completed phases
            val completed = tracker.getAllCompleted()

            // THEN: Should return both phases
            assertEquals(2, completed.size, "Should have 2 completed phases")
            assertTrue(completed.values.any { it.name == "DISCOVERY" })
            assertTrue(completed.values.any { it.name == "ANALYSIS" })
        }

    @Test
    fun `GIVEN tracker with data WHEN resetting THEN should clear all phases`() =
        runTest {
            // GIVEN: Tracker with active and completed phases
            val tracker = PhaseTracker()
            val activeId = tracker.startPhase("ACTIVE")
            val completedId = tracker.startPhase("COMPLETED")
            tracker.endPhase(completedId)

            // WHEN: Resetting the tracker
            tracker.reset()

            // THEN: Should clear everything
            assertEquals(0, tracker.activePhaseCount(), "Should have no active phases")
            assertEquals(0, tracker.getAllCompleted().size, "Should have no completed phases")
            assertFalse(tracker.isPhaseActive(activeId), "Previously active phase should not be active")
        }

    @Test
    fun `GIVEN phase metrics WHEN formatting duration THEN should use correct units`() =
        runTest {
            // GIVEN: Phase tracker with different durations
            val tracker = PhaseTracker()

            // Short duration (milliseconds)
            val shortId = tracker.startPhase("SHORT")
            Thread.sleep(50)
            val shortMetrics = tracker.endPhase(shortId)

            // WHEN: Formatting durations
            val shortFormatted = shortMetrics.formattedDuration()

            // THEN: Should use correct units
            assertTrue(shortFormatted.endsWith("ms"), "Short duration should use milliseconds")
        }

    @Test
    fun `GIVEN deeply nested phases WHEN ending phases THEN should maintain hierarchy`() =
        runTest {
            // GIVEN: 3-level nested structure
            val tracker = PhaseTracker()
            val level1 = tracker.startPhase("LEVEL1")
            val level2 = tracker.startPhase("LEVEL2", parent = level1)
            val level3 = tracker.startPhase("LEVEL3", parent = level2)

            Thread.sleep(5)

            // WHEN: Ending phases from innermost to outermost
            tracker.endPhase(level3)
            val level2Metrics = tracker.endPhase(level2)
            val level1Metrics = tracker.endPhase(level1)

            // THEN: Should maintain proper hierarchy
            assertEquals("LEVEL1", level1Metrics.name)
            assertEquals(1, level1Metrics.subPhases.size, "LEVEL1 should have 1 sub-phase")
            assertEquals("LEVEL2", level1Metrics.subPhases[0].name)
            assertEquals(1, level1Metrics.subPhases[0].subPhases.size, "LEVEL2 should have 1 sub-phase")
            assertEquals("LEVEL3", level1Metrics.subPhases[0].subPhases[0].name)
        }
}
