// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.config.FaktOptions
import com.rsicarelli.fakt.compiler.fir.FaktFirExtensionRegistrar
import com.rsicarelli.fakt.compiler.ir.UnifiedFaktIrGenerationExtension
import com.rsicarelli.fakt.compiler.telemetry.FaktLogger
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
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

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector =
            configuration.get(
                CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE,
            )
        val options = FaktOptions.load(configuration)

        // Create FaktLogger with configured log level
        val logger = FaktLogger(messageCollector, options.logLevel)
        val customAnnotations = listOf("com.rsicarelli.fakt.Fake")

        if (!options.enabled) {
            logger.trace("Plugin disabled, skipping registration")
            return
        }

        registerFirExtension(logger)
        registerIrExtension(logger, options, customAnnotations)
    }

    /**
     * Registers the FIR extension for @Fake annotation detection in the FIR phase.
     *
     * @param logger The FaktLogger for logging
     */
    private fun ExtensionStorage.registerFirExtension(logger: FaktLogger) {
        FirExtensionRegistrarAdapter.registerExtension(FaktFirExtensionRegistrar())
    }

    /**
     * Registers the unified IR generation extension for fake implementation generation.
     *
     * @param logger The FaktLogger for logging
     * @param options The loaded plugin options
     */
    private fun ExtensionStorage.registerIrExtension(
        logger: FaktLogger,
        options: FaktOptions,
        customAnnotations: List<String>,
    ) {
        IrGenerationExtension.registerExtension(
            UnifiedFaktIrGenerationExtension(
                logger = logger,
                outputDir = options.outputDir,
                fakeAnnotations = customAnnotations,
            ),
        )
    }
}
