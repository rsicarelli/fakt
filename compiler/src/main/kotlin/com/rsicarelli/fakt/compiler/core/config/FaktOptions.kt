// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.config

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Configuration options for the Fakt compiler plugin.
 * Loaded from Gradle plugin configuration and command line arguments.
 *
 * **Visibility**: Public to allow FIR/IR extensions to access configuration
 */
data class FaktOptions(
    val enabled: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    val outputDir: String? = null,
    val sourceSetContext: SourceSetContext? = null,
) {
    companion object {
        fun load(configuration: CompilerConfiguration): FaktOptions {
            // Load configuration from the command line processor
            // Default to enabled=true when not explicitly configured
            val enabled = configuration.get(FaktCommandLineProcessor.ENABLED_KEY) ?: true
            val logLevelString = configuration.get(FaktCommandLineProcessor.LOG_LEVEL_KEY)
            val outputDir = configuration.get(FaktCommandLineProcessor.OUTPUT_DIR_KEY)
            val sourceSetContext =
                configuration.get(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY)

            // Determine log level: use logLevel if present, otherwise default to INFO
            val logLevel = logLevelString?.let { LogLevel.fromString(it) } ?: LogLevel.INFO

            return FaktOptions(
                enabled = enabled,
                logLevel = logLevel,
                outputDir = outputDir,
                sourceSetContext = sourceSetContext,
            )
        }
    }

    override fun toString(): String =
        """
        FaktOptions(
            enabled=$enabled,
            logLevel=$logLevel,
            sourceSetContext=${sourceSetContext?.let { "present(${it.compilationName}/${it.targetName})" } ?: "null"}
        )
        """.trimIndent()
}
