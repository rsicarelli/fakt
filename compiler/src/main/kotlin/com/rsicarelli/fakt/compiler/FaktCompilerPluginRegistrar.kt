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

        if (!options.enabled) {
            logger.trace("Plugin disabled, skipping registration")
            return
        }

        // Create shared context for FIR→IR communication (Metro pattern)
        val sharedContext =
            FaktSharedContext(
                fakeAnnotations = FaktSharedContext.DEFAULT_FAKE_ANNOTATIONS,
                options = options,
                metadataStorage =
                    com.rsicarelli.fakt.compiler.fir
                        .FirMetadataStorage(),
            )

        registerFirExtension(logger, sharedContext)
        registerIrExtension(logger, sharedContext)
    }

    /**
     * Registers the FIR extension for @Fake annotation detection in the FIR phase.
     *
     * Following Metro pattern: Pass shared context to FIR extension.
     *
     * @param logger The FaktLogger for logging
     * @param sharedContext Shared context for FIR→IR communication
     */
    private fun ExtensionStorage.registerFirExtension(
        logger: FaktLogger,
        sharedContext: FaktSharedContext,
    ) {
        logger.trace("Registering FIR extension")
        FirExtensionRegistrarAdapter.registerExtension(FaktFirExtensionRegistrar(sharedContext))
    }

    /**
     * Registers the unified IR generation extension for fake implementation generation.
     *
     * Following Metro pattern: Pass shared context to IR extension for FIR metadata access.
     *
     * @param logger The FaktLogger for logging
     * @param sharedContext Shared context for FIR→IR communication
     */
    private fun ExtensionStorage.registerIrExtension(
        logger: FaktLogger,
        sharedContext: FaktSharedContext,
    ) {
        logger.trace("Registering IR extension with FIR metadata access")
        IrGenerationExtension.registerExtension(
            UnifiedFaktIrGenerationExtension(
                logger = logger,
                sharedContext = sharedContext,
            ),
        )
    }
}
