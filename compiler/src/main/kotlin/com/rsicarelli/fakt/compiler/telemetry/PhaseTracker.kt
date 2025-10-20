// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

import com.rsicarelli.fakt.compiler.telemetry.metrics.PhaseMetrics
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks compilation phase timing with support for nested sub-phases.
 *
 * **Thread-safe:** Uses ConcurrentHashMap for multi-threaded compilation.
 *
 * **Usage:**
 * ```kotlin
 * val tracker = PhaseTracker()
 *
 * // Top-level phase
 * val discoveryId = tracker.startPhase("DISCOVERY")
 * // ... discovery logic ...
 * val discoveryMetrics = tracker.endPhase(discoveryId)
 *
 * // Nested sub-phase
 * val analysisId = tracker.startPhase("ANALYSIS")
 * val interfaceId = tracker.startPhase("PredicateCombiner", parent = analysisId)
 * // ... analyze interface ...
 * tracker.endPhase(interfaceId)
 * val analysisMetrics = tracker.endPhase(analysisId)
 * ```
 *
 * **Design:**
 * - Each phase gets a unique ID (UUID)
 * - Phases track start time, end time, and sub-phases
 * - Supports hierarchical nesting for detailed breakdowns
 * - Thread-safe for parallel compilation
 *
 * @see PhaseMetrics
 */
class PhaseTracker {
    /**
     * Internal representation of a phase while it's being tracked.
     */
    private data class Phase(
        val id: String,
        val name: String,
        val startTime: Long,
        val parentId: String? = null,
        val subPhaseIds: MutableList<String> = mutableListOf(),
    )

    // Thread-safe storage for active phases
    private val activePhases = ConcurrentHashMap<String, Phase>()

    // Thread-safe storage for completed phase metrics
    private val completedPhases = ConcurrentHashMap<String, PhaseMetrics>()

    /**
     * Starts tracking a new phase.
     *
     * @param name Human-readable phase name (e.g., "DISCOVERY", "ANALYSIS")
     * @param parent Optional parent phase ID for nested tracking
     * @return Unique phase ID to use when calling endPhase()
     *
     * **Example:**
     * ```kotlin
     * val phaseId = tracker.startPhase("GENERATION")
     * // ... generation logic ...
     * tracker.endPhase(phaseId)
     * ```
     */
    fun startPhase(
        name: String,
        parent: String? = null,
    ): String {
        val id = UUID.randomUUID().toString()
        val phase =
            Phase(
                id = id,
                name = name,
                startTime = System.nanoTime(),
                parentId = parent,
            )

        activePhases[id] = phase

        // Register as sub-phase of parent if applicable
        parent?.let { parentId ->
            activePhases[parentId]?.subPhaseIds?.add(id)
        }

        return id
    }

    /**
     * Ends tracking of a phase and returns its metrics.
     *
     * Calculates duration and collects all sub-phase metrics.
     * Removes the phase from active tracking.
     *
     * @param phaseId The ID returned from startPhase()
     * @return PhaseMetrics with timing and sub-phase data
     * @throws IllegalStateException if phase ID is invalid or already ended
     *
     * **Example:**
     * ```kotlin
     * val phaseId = tracker.startPhase("ANALYSIS")
     * // ... analysis logic ...
     * val metrics = tracker.endPhase(phaseId)
     * println("Analysis took ${metrics.duration}ms")
     * ```
     */
    fun endPhase(phaseId: String): PhaseMetrics {
        val phase = activePhases.remove(phaseId) ?: error("Phase $phaseId not found or already ended")

        val endTime = System.nanoTime()

        // Collect all sub-phase metrics (must be completed first)
        val subPhaseMetrics =
            phase.subPhaseIds.mapNotNull { subPhaseId ->
                completedPhases[subPhaseId]
            }

        val metrics =
            PhaseMetrics(
                name = phase.name,
                startTime = phase.startTime,
                endTime = endTime,
                subPhases = subPhaseMetrics,
            )

        // Store completed metrics for parent phases to reference
        completedPhases[phaseId] = metrics

        return metrics
    }

    /**
     * Gets all completed phase metrics.
     *
     * Useful for final reporting after compilation.
     *
     * @return Map of phase ID to metrics
     */
    fun getAllCompleted(): Map<String, PhaseMetrics> = completedPhases.toMap()

    /**
     * Resets the tracker, clearing all active and completed phases.
     *
     * Useful for testing or starting a fresh compilation session.
     */
    fun reset() {
        activePhases.clear()
        completedPhases.clear()
    }

    /**
     * Checks if a phase is currently active (started but not ended).
     *
     * @param phaseId The phase ID to check
     * @return true if phase is active, false otherwise
     */
    fun isPhaseActive(phaseId: String): Boolean = activePhases.containsKey(phaseId)

    /**
     * Gets the number of currently active phases.
     *
     * Useful for debugging phase tracking.
     *
     * @return Number of active phases
     */
    fun activePhaseCount(): Int = activePhases.size
}
