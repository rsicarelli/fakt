// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

import com.rsicarelli.fakt.compiler.telemetry.metrics.CompilationSummary
import com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics
import com.rsicarelli.fakt.compiler.telemetry.metrics.PhaseMetrics

/**
 * Main telemetry facade for the Fakt compiler plugin.
 *
 * Coordinates logging, phase tracking, and metric collection throughout compilation.
 * Provides a simple API to instrument the compiler plugin with minimal boilerplate.
 *
 * **Usage Pattern:**
 * ```kotlin
 * // 1. Initialize telemetry
 * val telemetry = FaktTelemetry.initialize(logger)
 *
 * // 2. Track compilation with automatic timing
 * val summary = telemetry.trackCompilation {
 *     // 3. Track individual phases
 *     val discoveryId = telemetry.startPhase("DISCOVERY")
 *     val interfaces = discoverInterfaces()
 *     telemetry.metricsCollector.incrementInterfacesDiscovered(interfaces.size)
 *     telemetry.endPhase(discoveryId)
 *
 *     // 4. Record per-interface metrics
 *     interfaces.forEach { interface ->
 *         val metrics = processInterface(interface)
 *         telemetry.recordInterfaceMetrics(metrics)
 *     }
 * }
 *
 * // 5. Generate report
 * telemetry.logger.info(CompilationReport.generate(summary, telemetry.logger.logLevel))
 * ```
 *
 * **Design:**
 * - Singleton pattern via companion object
 * - Lazy initialization
 * - Thread-safe metric collection
 * - Automatic timing and aggregation
 *
 * @property logger The FaktLogger instance for level-aware logging
 * @property phaseTracker Tracks compilation phase timing
 * @property metricsCollector Collects and aggregates metrics
 */
class FaktTelemetry
    private constructor(
        val logger: FaktLogger,
    ) {
        val phaseTracker = PhaseTracker()
        val metricsCollector = MetricsCollector()

        /**
         * Tracks an entire compilation session with automatic timing.
         *
         * Wraps the compilation block, tracks total time, and builds a final summary.
         *
         * @param outputDirectory Output directory for generated files
         * @param block The compilation logic to execute
         * @return Complete compilation summary
         *
         * **Example:**
         * ```kotlin
         * val summary = telemetry.trackCompilation("build/generated/fakt/test") {
         *     // Compilation logic here
         *     val phaseId = telemetry.startPhase("DISCOVERY")
         *     // ... discovery logic ...
         *     telemetry.endPhase(phaseId)
         * }
         * ```
         */
        inline fun trackCompilation(
            outputDirectory: String = "auto-detect",
            block: () -> Unit,
        ): CompilationSummary {
            val startTime = System.nanoTime()

            try {
                block()
            } finally {
                val endTime = System.nanoTime()
                val totalTime = endTime - startTime

                // Build phase breakdown from completed phases
                val phaseBreakdown =
                    phaseTracker
                        .getAllCompleted()
                        .mapKeys { (_, metrics) -> metrics.name }

                return metricsCollector.buildSummary(
                    totalTimeNanos = totalTime,
                    phaseBreakdown = phaseBreakdown,
                    outputDirectory = outputDirectory,
                )
            }
        }

        /**
         * Starts tracking a compilation phase.
         *
         * @param name Phase name (e.g., "DISCOVERY", "ANALYSIS")
         * @param parent Optional parent phase ID for nested tracking
         * @return Phase ID to use with endPhase()
         *
         * **Example:**
         * ```kotlin
         * val phaseId = telemetry.startPhase("ANALYSIS")
         * // ... analysis logic ...
         * telemetry.endPhase(phaseId)
         * ```
         */
        fun startPhase(
            name: String,
            parent: String? = null,
        ): String {
            return phaseTracker.startPhase(name, parent)
        }

        /**
         * Ends tracking of a phase and returns its metrics.
         *
         * @param phaseId The ID returned from startPhase()
         * @return PhaseMetrics with timing data
         *
         * **Example:**
         * ```kotlin
         * val phaseId = telemetry.startPhase("GENERATION")
         * // ... generation logic ...
         * val metrics = telemetry.endPhase(phaseId)
         * logger.debug("Generation took ${metrics.duration}ms")
         * ```
         */
        fun endPhase(phaseId: String): PhaseMetrics {
            return phaseTracker.endPhase(phaseId)
        }

        /**
         * Records metrics for a processed fake (interface or class).
         *
         * Updates aggregate metrics and stores per-fake data.
         *
         * @param metrics The fake metrics to record
         *
         * **Example:**
         * ```kotlin
         * telemetry.recordFakeMetrics(
         *     FakeMetrics(
         *         name = "UserService",
         *         pattern = GenericPattern.NoGenerics,
         *         memberCount = 5,
         *         typeParamCount = 0,
         *         analysisTimeMs = 18,
         *         generationTimeMs = 45,
         *         generatedLOC = 87,
         *         importCount = 3
         *     )
         * )
         * ```
         */
        fun recordFakeMetrics(metrics: FakeMetrics) {
            metricsCollector.recordFakeMetrics(metrics)
        }

        /**
         * Resets all telemetry state.
         *
         * Useful for testing or starting a fresh compilation session.
         */
        fun reset() {
            phaseTracker.reset()
            metricsCollector.reset()
        }

        companion object {
            /**
             * Current telemetry instance (thread-local).
             *
             * Allows access to telemetry from anywhere in the compiler plugin.
             */
            private val threadLocalInstance = ThreadLocal<FaktTelemetry>()

            /**
             * Initializes or retrieves the telemetry instance.
             *
             * Creates a new instance if not already initialized for this thread.
             *
             * @param logger The FaktLogger to use for logging
             * @return The telemetry instance
             *
             * **Example:**
             * ```kotlin
             * val logger = FaktLogger(messageCollector, LogLevel.DEBUG)
             * val telemetry = FaktTelemetry.initialize(logger)
             * ```
             */
            fun initialize(logger: FaktLogger): FaktTelemetry {
                val existing = threadLocalInstance.get()
                if (existing != null) {
                    return existing
                }

                val telemetry = FaktTelemetry(logger)
                threadLocalInstance.set(telemetry)
                return telemetry
            }

            /**
             * Gets the current telemetry instance for this thread.
             *
             * @return The current telemetry instance, or null if not initialized
             *
             * **Example:**
             * ```kotlin
             * val telemetry = FaktTelemetry.current()
             * telemetry?.startPhase("DISCOVERY")
             * ```
             */
            fun current(): FaktTelemetry? = threadLocalInstance.get()

            /**
             * Clears the current telemetry instance.
             *
             * Useful for testing or cleanup.
             */
            fun clear() {
                threadLocalInstance.remove()
            }
        }
    }
