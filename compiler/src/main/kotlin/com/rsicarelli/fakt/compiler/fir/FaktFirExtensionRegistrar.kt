// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import com.rsicarelli.fakt.compiler.fir.checkers.FaktFirCheckers
import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * FIR extension registrar for Fakt compiler plugin.
 *
 * **Registered Extensions**:
 * - [FaktFirCheckers] - Validates @Fake usage and stores metadata
 *
 * @property sharedContext Shared context for FIR→IR communication
 */
class FaktFirExtensionRegistrar(
    private val sharedContext: FaktSharedContext,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() =
        +{ session: FirSession -> FaktFirCheckers.create(session, sharedContext) }
}
