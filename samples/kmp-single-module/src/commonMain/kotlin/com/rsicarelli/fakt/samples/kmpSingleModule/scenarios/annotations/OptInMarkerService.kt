// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import com.rsicarelli.fakt.Fake

/**
 * Service marked with custom @RequiresOptIn annotation.
 *
 * This tests that generated fakes automatically add @OptIn(ExperimentalFaktApi::class)
 * since the interface is marked with a @RequiresOptIn marker annotation.
 *
 * Without the @OptIn, the generated code would fail to compile with:
 * "This declaration needs opt-in. Its usage must be marked with @ExperimentalFaktApi"
 */
@ExperimentalFaktApi
@Fake
interface OptInMarkerService {
    fun experimentalOperation(): String
}

/**
 * Service marked with ERROR-level @RequiresOptIn annotation.
 *
 * This tests that the @OptIn is generated even for ERROR-level markers.
 */
@BetaFaktApi
@Fake
interface BetaService {
    fun betaFeature(): Int
}
