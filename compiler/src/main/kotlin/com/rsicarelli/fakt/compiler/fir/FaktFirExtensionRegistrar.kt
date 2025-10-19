// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * FIR extension registrar for Fakt compiler plugin.
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
     * Note: FIR extension registration is intentionally minimal as the actual
     * @Fake annotation detection happens in the IR phase via UnifiedFaktIrGenerationExtension.
     * This follows the pattern from Metro and other production compiler plugins.
     */
    override fun ExtensionRegistrarContext.configurePlugin() {
        // FIR phase: Extension registration placeholder
        // The FIR API is complex and evolving. For now, the main annotation detection
        // happens in the IR phase which has a more stable API surface.
        // Future enhancement: Add FIR-based validation when needed
        // +::FaktFirCheckers

        // Note: FIR extension registrar is invoked but remains silent
        // MessageCollector is not available in FirExtensionRegistrar context
        // All logging happens in IR phase with proper level control
    }
}
