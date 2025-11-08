// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import com.rsicarelli.fakt.compiler.FaktSharedContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * FIR extension registrar for Fakt compiler plugin.
 *
 * Following Metro's two-phase approach (see MetroFirExtensionRegistrar.kt:33-70):
 * 1. **FIR Phase** (THIS): Detect @Fake annotations and validate declarations
 * 2. **IR Phase**: Generate fake implementations from validated metadata
 *
 * **Metro Alignment**:
 * ```kotlin
 * // Metro pattern: MetroFirExtensionRegistrar.kt:38-39
 * +MetroFirBuiltIns.getFactory(classIds, options)
 * +::MetroFirCheckers
 * ```
 *
 * **Registered Extensions**:
 * - [FaktFirCheckers] - Validates @Fake usage and stores metadata
 *
 * @property sharedContext Shared context for FIR→IR communication
 */
class FaktFirExtensionRegistrar(
    private val sharedContext: FaktSharedContext,
) : FirExtensionRegistrar() {
    /**
     * Configure FIR plugin extensions.
     *
     * Following Metro pattern: Register checkers for validation.
     * The unary plus (+) operator registers the extension.
     */
    override fun ExtensionRegistrarContext.configurePlugin() {
        // Only register if FIR analysis is enabled
        if (!sharedContext.useFirAnalysis()) {
            // Legacy mode: FIR does nothing, IR handles everything
            return
        }

        // Register FIR checkers for @Fake validation
        // Following Metro pattern: +::MetroFirCheckers (MetroFirExtensionRegistrar.kt:39)
        +{ session: FirSession -> FaktFirCheckers.create(session, sharedContext) }
    }
}
