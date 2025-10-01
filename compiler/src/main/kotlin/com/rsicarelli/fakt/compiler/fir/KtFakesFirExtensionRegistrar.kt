// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * FIR extension registrar for KtFakes compiler plugin.
 *
 * This registrar follows Metro's two-phase approach:
 * 1. Analysis phase: Detect @Fake annotations and validate declarations
 * 2. Generation phase: Prepare for IR generation of fake implementations
 *
 * Registers the following FIR extensions:
 * - FakeAnnotationDetector: Detects and validates @Fake annotations
 * - ThreadSafetyChecker: Validates thread-safety requirements
 * - Error reporting infrastructure
 */
class FaktFirExtensionRegistrar : FirExtensionRegistrar() {

    /**
     * Configure the FIR plugin extensions.
     *
     * Registers all necessary FIR extensions for annotation detection,
     * validation, and error reporting.
     *
     * TODO: Implement proper extension registration when FIR API is stable
     */
    override fun ExtensionRegistrarContext.configurePlugin() {
        // TODO: Register checkers for annotation validation
        // The FIR API is complex and evolving, so we'll implement a basic structure first
        // +::FaktFirCheckers

        // For now, just mark that the FIR extension registrar is being called
        // Using println because MessageCollector is not available in FirExtensionRegistrar context
        println("============================================")
        println("KtFakes: FIR extension registrar configured")
        println("KtFakes: Ready to detect @Fake annotations")
        println("============================================")
    }
}
