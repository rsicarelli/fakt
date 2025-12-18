// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.core.config.FaktOptions
import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import com.rsicarelli.fakt.compiler.core.optimization.CompilerOptimizations
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.fir.FaktFirExtensionRegistrar
import com.rsicarelli.fakt.compiler.fir.cache.MetadataCacheManager
import com.rsicarelli.fakt.compiler.fir.metadata.FirMetadataStorage
import com.rsicarelli.fakt.compiler.ir.generation.UnifiedFaktIrGenerationExtension
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
class FaktCompilerPluginRegistrar : CompilerPluginRegistrar() {
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
            logger.info("Plugin disabled, skipping registration")
            return
        }

        // Initialize compiler optimizations for caching and incremental compilation
        val fakeAnnotations = FaktSharedContext.DEFAULT_FAKE_ANNOTATIONS
        val optimizations =
            CompilerOptimizations(
                fakeAnnotations = fakeAnnotations,
                outputDir = options.outputDir,
                logger = logger,
            )

        // Initialize cache manager for KMP cross-compilation caching (if configured)
        val cacheManager = createCacheManager(options, logger)

        // Determine cache mode for logging
        val cacheMode = when {
            cacheManager?.isProducerMode == true -> "PRODUCER → ${options.metadataOutputPath}"
            cacheManager?.isConsumerMode == true -> "CONSUMER ← ${options.metadataCachePath}"
            else -> "disabled"
        }

        // Log consolidated initialization tree
        logPluginInitialization(
            logger = logger,
            options = options,
            fakeAnnotations = fakeAnnotations,
            optimizations = optimizations,
            cacheMode = cacheMode,
        )

        val sharedContext =
            FaktSharedContext(
                fakeAnnotations = fakeAnnotations,
                options = options,
                metadataStorage = FirMetadataStorage(),
                logger = logger,
                optimizations = optimizations,
                cacheManager = cacheManager,
            )

        registerFirExtension(sharedContext)
        registerIrExtension(sharedContext)
    }

    /**
     * Registers the FIR extension for @Fake annotation detection in the FIR phase.
     *
     * Following Metro pattern: Pass shared context (with logger and telemetry) to FIR extension.
     *
     * @param sharedContext Shared context for FIR→IR communication
     */
    private fun ExtensionStorage.registerFirExtension(sharedContext: FaktSharedContext) {
        sharedContext.logger.debug("Registering FIR extension")
        FirExtensionRegistrarAdapter.registerExtension(FaktFirExtensionRegistrar(sharedContext))
    }

    /**
     * Registers the unified IR generation extension for fake implementation generation.
     *
     * Following Metro pattern: Pass shared context (with logger and telemetry) to IR extension.
     *
     * @param sharedContext Shared context for FIR→IR communication
     */
    private fun ExtensionStorage.registerIrExtension(sharedContext: FaktSharedContext) {
        sharedContext.logger.debug("Registering IR extension")
        IrGenerationExtension.registerExtension(
            UnifiedFaktIrGenerationExtension(sharedContext),
        )
    }

    /**
     * Logs plugin initialization details at DEBUG level.
     *
     * This is called AFTER cache manager creation to include all info in one tree.
     *
     * @param logger Logger instance for output
     * @param options Compiler plugin options
     * @param fakeAnnotations List of configured @Fake annotation FQNs
     * @param optimizations Compiler optimizations for cache size reporting
     * @param cacheMode Cache mode description (e.g., "PRODUCER → path", "CONSUMER ← path", "disabled")
     */
    private fun logPluginInitialization(
        logger: FaktLogger,
        options: FaktOptions,
        fakeAnnotations: List<String>,
        optimizations: CompilerOptimizations,
        cacheMode: String,
    ) = logger.debug(
        buildString {
            appendLine("Fakt Plugin initialized")
            appendLine("   ├─ enabled: ${options.enabled}")
            appendLine("   ├─ logLevel: ${options.logLevel}")
            appendLine("   ├─ annotations: ${fakeAnnotations.joinToString(", ")}")
            appendLine("   ├─ output: ${options.outputDir}")
            appendLine("   ├─ firCacheMode: $cacheMode")
            append("   └─ irCacheSize: ${optimizations.cacheSize()} signatures")
        }
    )

    /**
     * Creates a cache manager for KMP cross-compilation caching.
     *
     * Returns null if neither producer nor consumer mode is configured,
     * which is the case for non-KMP projects or when caching is disabled.
     *
     * @param options Compiler options containing cache paths
     * @param logger Logger for cache operation messages
     * @return MetadataCacheManager if caching is configured, null otherwise
     */
    private fun createCacheManager(
        options: FaktOptions,
        logger: FaktLogger,
    ): MetadataCacheManager? {
        val metadataOutputPath = options.metadataOutputPath
        val metadataCachePath = options.metadataCachePath

        // Only create cache manager if caching is configured
        if (metadataOutputPath == null && metadataCachePath == null) {
            return null
        }

        return MetadataCacheManager(
            metadataOutputPath = metadataOutputPath,
            metadataCachePath = metadataCachePath,
            logger = logger,
        )
    }
}
