// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.TimeFormatter
import com.rsicarelli.fakt.compiler.core.optimization.CompilerOptimizations
import com.rsicarelli.fakt.compiler.core.telemetry.CompilationReport
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.telemetry.FaktTelemetry

/**
 * Logging utilities for IR generation phase.
 */
internal object IrGenerationLogging {
    private var headerLogged = false
    private val headerLock = Any()

    /**
     * Logs the plugin initialization header once per compilation.
     *
     * Thread-safe: Uses synchronized block to ensure header is logged only once
     * even in multi-threaded compilation scenarios.
     *
     * @param logger The FaktLogger instance
     * @param fakeAnnotations List of detected @Fake annotations
     * @param outputDir Optional output directory path
     * @param optimizations Compiler optimizations for cache size reporting
     */
    fun logHeaderOnce(
        logger: FaktLogger,
        fakeAnnotations: List<String>,
        outputDir: String?,
        optimizations: CompilerOptimizations,
    ) {
        synchronized(headerLock) {
            if (headerLogged) return
            headerLogged = true

            logger.trace("════════════════════════════════════════════════════════════")
            logger.trace("Fakt Plugin initialized")
            logger.trace("├─ enabled: true")
            logger.trace("├─ logLevel: ${logger.logLevel}")
            logger.trace("├─ detectedAnnotations: ${fakeAnnotations.joinToString(", ")}")
            if (outputDir != null) {
                val simplifiedPath =
                    outputDir
                        .substringAfter("/ktfakes/samples/", "")
                        .ifEmpty { outputDir }
                logger.trace("├─ output: $simplifiedPath")
            }
            logger.trace("└─ cache: ${optimizations.cacheSize()} signatures loaded")
            logger.trace("════════════════════════════════════════════════════════════")
        }
    }

    /**
     * Logs compilation completion summary with timing breakdown and metrics.
     *
     * Generates and displays a compilation report based on the configured log level:
     * - QUIET: No output
     * - INFO: Summary statistics
     * - DEBUG: Detailed breakdown
     * - TRACE: Full tree-style output
     *
     * @param logger The FaktLogger instance
     * @param telemetry Telemetry instance for phase tracking and metrics
     * @param outputDir Optional output directory for reporting
     */
    fun logGenerationCompletion(
        logger: FaktLogger,
        telemetry: FaktTelemetry,
        outputDir: String?,
    ) {
        // Calculate total time from all completed phases
        val totalTime =
            telemetry.phaseTracker
                .getAllCompleted()
                .values
                .sumOf { it.duration }

        // Generate compilation report
        val summary =
            telemetry.metricsCollector.buildSummary(
                totalTimeNanos = totalTime,
                phaseBreakdown = telemetry.phaseTracker.getAllCompleted().mapKeys { it.value.name },
                outputDirectory = outputDir ?: "auto-detect",
            )

        // Log report based on level
        val report = CompilationReport.generate(summary, logger.logLevel)
        if (report.isNotEmpty()) {
            report
                .lines()
                .filter { it.isNotBlank() } // Skip empty lines
                .forEach { line ->
                    // Use trace() for TRACE level to avoid "Fakt:" prefix
                    if (logger.logLevel >= LogLevel.TRACE) {
                        logger.trace(line)
                    } else {
                        logger.info(line)
                    }
                }
        }
    }

    /**
     * Logs generation errors with context.
     *
     * @param logger The FaktLogger instance
     * @param exception The exception that occurred during generation
     */
    fun logGenerationError(
        logger: FaktLogger,
        exception: Exception,
    ) {
        logger.error("IR-native generation failed: ${exception.message}")
    }

    /**
     * Logs tree-style processing details for a single fake (TRACE level only).
     *
     * Example output:
     * ```
     * UserRepository
     * ├─ Analysis: 3 type parameters, 5 members (120μs)
     * ├─ Generation: FakeUserRepositoryImpl (45 LOC, 2.5ms)
     * └─ Output: com/example/FakeUserRepositoryImpl.kt
     * ```
     *
     * @param logger The FaktLogger instance
     * @param name Interface or class name
     * @param analysisTimeNanos Analysis phase duration in nanoseconds
     * @param generationTimeNanos Generation phase duration in nanoseconds
     * @param loc Lines of code generated
     * @param outputPath Absolute or relative path to generated file
     * @param analysisDetail Optional analysis details (e.g., "3 type parameters, 5 members")
     */
    fun logFakeProcessing(
        logger: FaktLogger,
        name: String,
        analysisTimeNanos: Long,
        generationTimeNanos: Long,
        loc: Int,
        outputPath: String,
        analysisDetail: String = "",
    ) {
        logger.trace(name)
        val analysisTime = TimeFormatter.format(analysisTimeNanos)
        val generationTime = TimeFormatter.format(generationTimeNanos)

        if (analysisDetail.isNotEmpty()) {
            logger.trace("├─ Analysis: $analysisDetail ($analysisTime)")
        } else {
            logger.trace("├─ Analysis: $analysisTime")
        }

        logger.trace("├─ Generation: Fake${name}Impl ($loc LOC, $generationTime)")
        logger.trace("└─ Output: $outputPath")
    }
}
