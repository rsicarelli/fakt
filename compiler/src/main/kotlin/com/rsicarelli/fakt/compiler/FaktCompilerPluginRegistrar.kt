// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.config.FaktOptions
import com.rsicarelli.fakt.compiler.fir.FaktFirExtensionRegistrar
import com.rsicarelli.fakt.compiler.ir.UnifiedFaktIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/**
 * Main entry point for the Fakt compiler plugin with unified IR-native architecture.
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
public class FaktCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector =
            configuration.get(
                CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE,
            )

        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "============================================",
        )
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt: Compiler Plugin Registrar Invoked",
        )

        // Pass the configuration to the options loader
        val options = FaktOptions.load(configuration)

        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt: Plugin enabled: ${options.enabled}",
        )

        if (!options.enabled) {
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "Fakt: Plugin disabled, skipping registration",
            )
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "============================================",
            )
            return
        }

        if (options.debug) {
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "Fakt compiler plugin enabled with options: $options",
            )
        }

        // Register FIR extensions for analysis phase
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt: Registering FIR extension",
        )
        FirExtensionRegistrarAdapter.registerExtension(FaktFirExtensionRegistrar())

        // Register unified IR-native generation extension with custom annotation support
        val customAnnotations =
            listOf(
                "com.rsicarelli.fakt.Fake",
                "test.sample.CompanyTestDouble", // Test custom annotation
            )
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt: Registering IR generation extension",
        )
        IrGenerationExtension.registerExtension(
            UnifiedFaktIrGenerationExtension(
                messageCollector = messageCollector,
                outputDir = options.outputDir,
                fakeAnnotations = customAnnotations,
            ),
        )

        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt compiler plugin registered successfully",
        )
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "============================================",
        )
    }
}
