// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import dev.rsicarelli.ktfake.compiler.fir.KtFakesFirExtensionRegistrar
import dev.rsicarelli.ktfake.compiler.ir.KtFakesIrGenerationExtension

/**
 * Main entry point for the KtFake compiler plugin.
 *
 * This registrar follows Metro's two-phase approach:
 * 1. FIR extensions for analysis and validation
 * 2. IR extensions for code generation
 *
 * The plugin is only enabled when explicitly configured and supports K2 compilation.
 */
@OptIn(ExperimentalCompilerApi::class)
public class KtFakeCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = KtFakeOptions.load(configuration)

        if (!options.enabled) {
            return
        }

        val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        if (options.debug) {
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "KtFake compiler plugin enabled with options: $options"
            )
        }

        // Register FIR extensions for analysis phase
        FirExtensionRegistrarAdapter.registerExtension(KtFakesFirExtensionRegistrar())

        // Register IR extensions for code generation phase
        IrGenerationExtension.registerExtension(KtFakesIrGenerationExtension(messageCollector))

        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "KtFake compiler plugin registered successfully"
        )
    }
}

/**
 * Configuration options for the KtFake compiler plugin.
 * Loaded from Gradle plugin configuration and command line arguments.
 */
internal data class KtFakeOptions(
    val enabled: Boolean = false,
    val debug: Boolean = false,
    val generateCallTracking: Boolean = true,
    val generateBuilderPatterns: Boolean = true,
    val strictMode: Boolean = false
) {
    companion object {
        fun load(configuration: CompilerConfiguration): KtFakeOptions {
            // Load configuration from compiler arguments if available, otherwise use defaults
            // This is sufficient for MVP - more sophisticated config loading can be added later
            return KtFakeOptions(
                enabled = true,  // Always enabled for MVP
                debug = true     // Always debug for MVP
            )
        }
    }

    override fun toString(): String {
        return """
            KtFakeOptions(
                enabled=$enabled,
                debug=$debug,
                generateCallTracking=$generateCallTracking,
                generateBuilderPatterns=$generateBuilderPatterns,
                strictMode=$strictMode
            )
        """.trimIndent()
    }
}
