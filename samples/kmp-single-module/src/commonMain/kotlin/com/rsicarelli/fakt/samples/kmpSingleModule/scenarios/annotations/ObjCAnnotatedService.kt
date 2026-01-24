// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import com.rsicarelli.fakt.Fake
import kotlin.native.HiddenFromObjC

/**
 * Test case: @HiddenFromObjC annotation.
 *
 * The generated FakeHiddenServiceImpl should include:
 * ```kotlin
 * @HiddenFromObjC
 * class FakeHiddenServiceImpl : HiddenService { ... }
 * ```
 *
 * Tests:
 * - Kotlin/Native specific annotation propagation
 * - Annotation with no arguments
 * - Platform-specific annotation handling
 *
 * This annotation hides the class from Objective-C interop, which is useful
 * for internal implementation details that shouldn't be exposed to iOS/macOS.
 */
@Fake
@HiddenFromObjC
interface HiddenService {
    fun internalOperation(): String
    fun getInternalState(): Map<String, Any>
}

/**
 * Test case: @HiddenFromObjC combined with other annotations.
 *
 * Tests that HiddenFromObjC works alongside other annotations.
 */
@Fake
@HiddenFromObjC
@OptIn(ExperimentalFaktApi::class)
@FeatureFlag(name = "internal-api", enabled = true)
interface HiddenExperimentalService {
    fun performInternalExperiment(): Boolean
    suspend fun asyncInternalWork(): Int
}

/**
 * Test case: Abstract class with @HiddenFromObjC.
 *
 * Tests annotation propagation on abstract classes with HiddenFromObjC.
 */
@Fake
@HiddenFromObjC
@Tags("internal", "hidden")
abstract class HiddenRepository {
    abstract fun findInternal(key: String): String?
    abstract suspend fun saveInternal(key: String, value: String): Boolean
}
