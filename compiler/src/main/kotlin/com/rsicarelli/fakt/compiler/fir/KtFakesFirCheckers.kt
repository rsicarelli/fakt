// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.fir.FirSession

/**
 * FIR checkers for KtFakes compiler plugin.
 *
 * Provides validation checkers for @Fake annotations including:
 * - Thread safety validation (no object declarations)
 * - Annotation parameter validation
 * - Declaration type validation (interfaces and classes only)
 *
 * TODO: Implement proper FirAdditionalCheckersExtension when API is stable
 */
class FaktFirCheckers(val session: FirSession) {

    val threadSafetyChecker = ThreadSafetyChecker()

    // TODO: Implement proper checker registration when FIR API is stable
}
