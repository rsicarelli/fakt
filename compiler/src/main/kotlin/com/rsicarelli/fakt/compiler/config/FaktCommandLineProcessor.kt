// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.config

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.util.Base64

/**
 * Command line processor for Fakt compiler plugin options.
 */
@OptIn(ExperimentalCompilerApi::class)
class FaktCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ENABLED_KEY = CompilerConfigurationKey<Boolean>("fakt.enabled")
        val LOG_LEVEL_KEY = CompilerConfigurationKey<String>("fakt.logLevel")
        val OUTPUT_DIR_KEY = CompilerConfigurationKey<String>("fakt.outputDir")
        val SOURCE_SET_CONTEXT_KEY =
            CompilerConfigurationKey<SourceSetContext>("fakt.sourceSetContext")

        val ENABLED_OPTION =
            CliOption(
                optionName = "enabled",
                valueDescription = "true|false",
                description = "Enable Fakt compiler plugin",
                required = false,
            )

        val LOG_LEVEL_OPTION =
            CliOption(
                optionName = "logLevel",
                valueDescription = "QUIET|INFO|DEBUG|TRACE",
                description = "Logging verbosity level (default: INFO)",
                required = false,
            )

        val OUTPUT_DIR_OPTION =
            CliOption(
                optionName = "outputDir",
                valueDescription = "path",
                description = "Output directory for generated fakes",
                required = false,
            )

        val SOURCE_SET_CONTEXT_OPTION =
            CliOption(
                optionName = "sourceSetContext",
                valueDescription = "base64-encoded-json",
                description = "Serialized source set context from Gradle plugin",
                required = false,
            )
    }

    override val pluginId: String = "com.rsicarelli.fakt"

    override val pluginOptions: Collection<CliOption> =
        listOf(
            ENABLED_OPTION,
            LOG_LEVEL_OPTION,
            OUTPUT_DIR_OPTION,
            SOURCE_SET_CONTEXT_OPTION,
        )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            "enabled" -> configuration.put(ENABLED_KEY, value.toBoolean())
            "logLevel" -> configuration.put(LOG_LEVEL_KEY, value)
            "outputDir" -> configuration.put(OUTPUT_DIR_KEY, value)
            "sourceSetContext" -> {
                val messageCollector =
                    configuration.get(
                        CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                        MessageCollector.NONE,
                    )

                @Suppress("TooGenericExceptionCaught")
                try {
                    // Decode Base64
                    val decodedBytes = Base64.getDecoder().decode(value)
                    val jsonString = String(decodedBytes)

                    // Deserialize JSON
                    val json = Json { ignoreUnknownKeys = true }
                    val context = json.decodeFromString<SourceSetContext>(jsonString)

                    configuration.put(SOURCE_SET_CONTEXT_KEY, context)
                } catch (e: Exception) {
                    // Report deserialization errors to help diagnose issues
                    messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "Fakt: Failed to deserialize sourceSetContext: ${e.javaClass.simpleName}: ${e.message}",
                    )
                }
            }
        }
    }
}
