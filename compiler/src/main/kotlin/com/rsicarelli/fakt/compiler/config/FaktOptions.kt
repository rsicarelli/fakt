// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.config

import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Configuration options for the Fakt compiler plugin.
 * Loaded from Gradle plugin configuration and command line arguments.
 */
internal data class FaktOptions(
    val enabled: Boolean = false,
    val debug: Boolean = false,
    val generateCallTracking: Boolean = true,
    val generateBuilderPatterns: Boolean = true,
    val strictMode: Boolean = false,
    val outputDir: String? = null,
) {
    companion object {
        fun load(configuration: CompilerConfiguration): FaktOptions {
            // Load configuration from the command line processor
            // Default to enabled=true when not explicitly configured
            val enabled = configuration.get(FaktCommandLineProcessor.ENABLED_KEY) ?: true
            val debug = configuration.get(FaktCommandLineProcessor.DEBUG_KEY) ?: true
            val outputDir = configuration.get(FaktCommandLineProcessor.OUTPUT_DIR_KEY)

            return FaktOptions(
                enabled = enabled,
                debug = debug,
                outputDir = outputDir,
            )
        }
    }

    override fun toString(): String =
        """
        FaktOptions(
            enabled=$enabled,
            debug=$debug,
            generateCallTracking=$generateCallTracking,
            generateBuilderPatterns=$generateBuilderPatterns,
            strictMode=$strictMode
        )
        """.trimIndent()
}
