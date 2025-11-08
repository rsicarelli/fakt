// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.config

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Configuration options for the Fakt compiler plugin.
 * Loaded from Gradle plugin configuration and command line arguments.
 *
 * **Modernization (v1.1.0)**:
 * - Added sourceSetContext for data-driven source set resolution
 * - Replaces hardcoded source set patterns with dynamic hierarchy
 *
 * **Telemetry (v1.2.0)**:
 * - Added logLevel for granular logging control
 *
 * **FIR/IR Separation (v1.3.0)**:
 * - Added useFirAnalysis feature flag for FIR-based validation
 * - Enables proper Metro-aligned two-phase architecture
 *
 * **Visibility**: Public to allow FIR/IR extensions to access configuration (Metro pattern)
 */
data class FaktOptions(
    val enabled: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    val outputDir: String? = null,
    val sourceSetContext: SourceSetContext? = null,
    /**
     * Enable FIR-based analysis and validation (Metro-aligned architecture).
     *
     * When **true** (new behavior):
     * - FIR phase validates @Fake annotations and analyzes interfaces
     * - IR phase only generates code from validated metadata
     * - Better error messages with accurate source locations
     * - Faster compilation (analysis once, generate many times)
     *
     * When **false** (legacy behavior, default for now):
     * - FIR phase does nothing
     * - IR phase performs discovery, validation, and generation
     * - Maintains backward compatibility
     *
     * **Migration Strategy**:
     * - Phase 2-5: Default false, implement and test dual-mode
     * - Phase 6: Default true after validation
     * - Phase 7: Remove legacy code, make this option obsolete
     *
     * @see com.rsicarelli.fakt.compiler.FaktSharedContext
     * @see com.rsicarelli.fakt.compiler.fir.FirMetadataStorage
     */
    val useFirAnalysis: Boolean = false,
) {
    companion object {
        fun load(configuration: CompilerConfiguration): FaktOptions {
            // Load configuration from the command line processor
            // Default to enabled=true when not explicitly configured
            val enabled = configuration.get(FaktCommandLineProcessor.ENABLED_KEY) ?: true
            val logLevelString = configuration.get(FaktCommandLineProcessor.LOG_LEVEL_KEY)
            val outputDir = configuration.get(FaktCommandLineProcessor.OUTPUT_DIR_KEY)
            val sourceSetContext = configuration.get(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY)
            val useFirAnalysis = configuration.get(FaktCommandLineProcessor.USE_FIR_ANALYSIS_KEY) ?: false

            // Determine log level: use logLevel if present, otherwise default to INFO
            val logLevel = logLevelString?.let { LogLevel.fromString(it) } ?: LogLevel.INFO

            return FaktOptions(
                enabled = enabled,
                logLevel = logLevel,
                outputDir = outputDir,
                sourceSetContext = sourceSetContext,
                useFirAnalysis = useFirAnalysis,
            )
        }
    }

    override fun toString(): String =
        """
        FaktOptions(
            enabled=$enabled,
            logLevel=$logLevel,
            useFirAnalysis=$useFirAnalysis,
            sourceSetContext=${sourceSetContext?.let { "present(${it.compilationName}/${it.targetName})" } ?: "null"}
        )
        """.trimIndent()
}
