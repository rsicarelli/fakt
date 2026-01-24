// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import com.rsicarelli.fakt.Fake

/**
 * Test case: @OptIn annotation with class reference argument.
 *
 * The generated FakeOptInServiceImpl should include:
 * ```kotlin
 * @OptIn(ExperimentalFaktApi::class)
 * class FakeOptInServiceImpl : OptInService { ... }
 * ```
 *
 * Expected behavior:
 * - Annotation is propagated to generated class
 * - Class reference argument (ExperimentalFaktApi::class) is correctly rendered
 * - Import for OptIn is added
 * - Import for ExperimentalFaktApi is added
 */
@Fake
@OptIn(ExperimentalFaktApi::class)
interface OptInService {
    fun performExperimentalOperation(): String
    suspend fun asyncExperimentalWork(): Int
}

/**
 * Test case: @OptIn with multiple experimental markers.
 *
 * The generated fake should include both OptIn arguments.
 */
@Fake
@OptIn(ExperimentalFaktApi::class, BetaFaktApi::class)
interface MultiOptInService {
    fun useBothApis(): Boolean
}
