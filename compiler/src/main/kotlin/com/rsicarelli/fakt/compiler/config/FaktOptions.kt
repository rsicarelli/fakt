// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.config

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Configuration options for the Fakt compiler plugin.
 * Loaded from Gradle plugin configuration and command line arguments.
 *
 * **Modernization (v1.1.0)**:
 * - Added sourceSetContext for data-driven source set resolution
 * - Replaces hardcoded source set patterns with dynamic hierarchy
 */
internal data class FaktOptions(
    val enabled: Boolean = false,
    val debug: Boolean = false,
    val outputDir: String? = null,
    val sourceSetContext: SourceSetContext? = null,
) {
    companion object {
        fun load(configuration: CompilerConfiguration): FaktOptions {
            // Load configuration from the command line processor
            // Default to enabled=true when not explicitly configured
            val enabled = configuration.get(FaktCommandLineProcessor.ENABLED_KEY) ?: true
            val debug = configuration.get(FaktCommandLineProcessor.DEBUG_KEY) ?: true
            val outputDir = configuration.get(FaktCommandLineProcessor.OUTPUT_DIR_KEY)
            val sourceSetContext = configuration.get(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY)

            return FaktOptions(
                enabled = enabled,
                debug = debug,
                outputDir = outputDir,
                sourceSetContext = sourceSetContext,
            )
        }
    }

    override fun toString(): String =
        """
        FaktOptions(
            enabled=$enabled,
            debug=$debug,
            sourceSetContext=${sourceSetContext?.let { "present(${it.compilationName}/${it.targetName})" } ?: "null"}
        )
        """.trimIndent()
}
