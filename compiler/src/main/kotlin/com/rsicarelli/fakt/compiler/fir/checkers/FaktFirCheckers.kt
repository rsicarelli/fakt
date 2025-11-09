// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.checkers

import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

/**
 * FIR checkers extension for @Fake annotation validation.
 *
 * @property session FIR compilation session
 * @property sharedContext Shared context with metadata storage
 */
class FaktFirCheckers(
    session: FirSession,
    private val sharedContext: FaktSharedContext,
) : FirAdditionalCheckersExtension(session) {
    /**
     * Register declaration checkers for @Fake validation.
     *
     * @return DeclarationCheckers with our custom class checkers
     */
    override val declarationCheckers: DeclarationCheckers =
        object : DeclarationCheckers() {
            override val classCheckers: Set<FirClassChecker> =
                setOf(
                    FakeInterfaceChecker(sharedContext),
                    FakeClassChecker(sharedContext),
                )
        }

    companion object {
        /**
         * Factory function for FirAdditionalCheckersExtension.
         * */
        fun create(
            session: FirSession,
            sharedContext: FaktSharedContext,
        ): FaktFirCheckers = FaktFirCheckers(session, sharedContext)
    }
}
