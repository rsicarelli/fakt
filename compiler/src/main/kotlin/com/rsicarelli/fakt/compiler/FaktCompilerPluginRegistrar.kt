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

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector =
            configuration.get(
                CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE,
            )
        val options = FaktOptions.load(configuration)

        reportPluginInvocation(messageCollector, options)

        if (!options.enabled) {
            reportPluginDisabled(messageCollector)
            return
        }

        registerFirExtension(messageCollector)
        registerIrExtension(messageCollector, options)

        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt compiler plugin registered successfully",
        )
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "============================================",
        )
    }

    /**
     * Reports plugin invocation status and configuration to the message collector.
     *
     * @param messageCollector The message collector for compiler output
     * @param options The loaded plugin options
     */
    private fun reportPluginInvocation(
        messageCollector: MessageCollector,
        options: FaktOptions,
    ) {
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "============================================",
        )
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt: Compiler Plugin Registrar Invoked",
        )
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt: Plugin enabled: ${options.enabled}",
        )
        if (options.debug) {
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "Fakt compiler plugin enabled with options: $options",
            )
        }
    }

    /**
     * Reports that the plugin is disabled and skips registration.
     *
     * @param messageCollector The message collector for compiler output
     */
    private fun reportPluginDisabled(messageCollector: MessageCollector) {
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt: Plugin disabled, skipping registration",
        )
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "============================================",
        )
    }

    /**
     * Registers the FIR extension for @Fake annotation detection in the FIR phase.
     *
     * @param messageCollector The message collector for compiler output
     */
    private fun ExtensionStorage.registerFirExtension(messageCollector: MessageCollector) {
        messageCollector.report(CompilerMessageSeverity.INFO, "Fakt: Registering FIR extension")
        FirExtensionRegistrarAdapter.registerExtension(FaktFirExtensionRegistrar())
    }

    /**
     * Registers the unified IR generation extension for fake implementation generation.
     *
     * @param messageCollector The message collector for compiler output
     * @param options The loaded plugin options
     */
    private fun ExtensionStorage.registerIrExtension(
        messageCollector: MessageCollector,
        options: FaktOptions,
    ) {
        val customAnnotations = listOf("com.rsicarelli.fakt.Fake")
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Fakt: Registering IR generation extension",
        )

        // Log source set context availability
        if (options.sourceSetContext != null) {
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "Fakt: Using SourceSetContext " +
                    "(${options.sourceSetContext.compilationName}/${options.sourceSetContext.targetName})",
            )
        } else {
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "Fakt: No SourceSetContext available, using legacy source set mapping",
            )
        }

        IrGenerationExtension.registerExtension(
            UnifiedFaktIrGenerationExtension(
                messageCollector = messageCollector,
                outputDir = options.outputDir,
                fakeAnnotations = customAnnotations,
                sourceSetContext = options.sourceSetContext,
            ),
        )
    }
}
