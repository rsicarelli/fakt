// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.fir

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
class KtFakesFirExtensionRegistrar : FirExtensionRegistrar() {

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
        // +::KtFakesFirCheckers

        // For now, just mark that the FIR extension registrar is being called
        println("KtFakes: FIR extension registrar configured")
    }
}
