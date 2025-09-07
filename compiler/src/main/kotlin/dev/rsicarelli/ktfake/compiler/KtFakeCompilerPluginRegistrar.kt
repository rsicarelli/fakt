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
import dev.rsicarelli.ktfake.compiler.UnifiedKtFakesIrGenerationExtension

/**
 * Main entry point for the KtFake compiler plugin with unified IR-native architecture.
 *
 * This registrar follows the two-phase approach:
 * 1. FIR extensions for @Fake annotation detection and validation
 * 2. Unified IR-native extension for type-safe code generation
 *
 * The plugin uses modular components from the unified architecture:
 * - InterfaceAnalyzer: Dynamic interface analysis
 * - IrCodeGenerator: Pure IR node generation 
 * - DiagnosticsReporter: Professional error reporting
 *
 * The plugin is only enabled when explicitly configured and supports K2 compilation.
 */
@OptIn(ExperimentalCompilerApi::class)
public class KtFakeCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Pass the configuration to the options loader
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

        // Register unified IR-native generation extension
        IrGenerationExtension.registerExtension(UnifiedKtFakesIrGenerationExtension(messageCollector, options.outputDir))

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
    val strictMode: Boolean = false,
    val outputDir: String? = null
) {
    companion object {
        fun load(configuration: CompilerConfiguration): KtFakeOptions {
            // Load configuration from the command line processor
            return KtFakeOptions(
                enabled = configuration.get(KtFakeCommandLineProcessor.ENABLED_KEY, true),
                debug = configuration.get(KtFakeCommandLineProcessor.DEBUG_KEY, true),
                outputDir = configuration.get(KtFakeCommandLineProcessor.OUTPUT_DIR_KEY)
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
