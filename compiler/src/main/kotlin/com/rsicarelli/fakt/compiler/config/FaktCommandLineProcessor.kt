// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.config

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

/**
 * Command line processor for Fakt compiler plugin options.
 */
@OptIn(ExperimentalCompilerApi::class)
class FaktCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ENABLED_KEY = CompilerConfigurationKey<Boolean>("fakt.enabled")
        val DEBUG_KEY = CompilerConfigurationKey<Boolean>("fakt.debug")
        val OUTPUT_DIR_KEY = CompilerConfigurationKey<String>("fakt.outputDir")

        val ENABLED_OPTION =
            CliOption(
                optionName = "enabled",
                valueDescription = "true|false",
                description = "Enable Fakt compiler plugin",
                required = false,
            )

        val DEBUG_OPTION =
            CliOption(
                optionName = "debug",
                valueDescription = "true|false",
                description = "Enable debug logging",
                required = false,
            )

        val OUTPUT_DIR_OPTION =
            CliOption(
                optionName = "outputDir",
                valueDescription = "path",
                description = "Output directory for generated fakes",
                required = false,
            )
    }

    override val pluginId: String = "com.rsicarelli.fakt"

    override val pluginOptions: Collection<CliOption> =
        listOf(
            ENABLED_OPTION,
            DEBUG_OPTION,
            OUTPUT_DIR_OPTION,
        )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            "enabled" -> configuration.put(ENABLED_KEY, value.toBoolean())
            "debug" -> configuration.put(DEBUG_KEY, value.toBoolean())
            "outputDir" -> configuration.put(OUTPUT_DIR_KEY, value)
        }
    }
}
