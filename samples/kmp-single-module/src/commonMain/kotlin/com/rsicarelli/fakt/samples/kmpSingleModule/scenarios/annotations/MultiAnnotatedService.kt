// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import com.rsicarelli.fakt.Fake

/**
 * Test case: Multiple annotations on a single interface.
 *
 * The generated FakeMultiAnnotatedServiceImpl should include:
 * ```kotlin
 * @OptIn(ExperimentalFaktApi::class)
 * @Deprecated("Use MultiAnnotatedServiceV2 instead")
 * @Suppress("DEPRECATION")
 * @FeatureFlag(name = "multi-annotated", enabled = true)
 * @Tags("legacy", "monitored")
 * class FakeMultiAnnotatedServiceImpl : MultiAnnotatedService { ... }
 * ```
 *
 * Tests:
 * - Multiple annotations on same type
 * - Correct ordering of annotations
 * - All argument types combined
 * - Import management for multiple annotations
 */
@Fake
@OptIn(ExperimentalFaktApi::class)
@Deprecated("Use MultiAnnotatedServiceV2 instead")
@Suppress("DEPRECATION")
@FeatureFlag(name = "multi-annotated", enabled = true)
@Tags("legacy", "monitored")
interface MultiAnnotatedService {
    fun performLegacyOperation(): String
    fun getStatus(): Int
}

/**
 * Test case: No-argument annotations combined with argument annotations.
 *
 * Tests mix of marker annotations and annotations with arguments.
 */
@Fake
@OptIn(ExperimentalFaktApi::class)
@Metrics(priority = 5)
interface MixedAnnotationService {
    fun process(): Boolean
}

/**
 * Test case: Same annotation type cannot be repeated, but different OptIns can.
 *
 * Tests proper handling when multiple experimental APIs are used.
 */
@Fake
@OptIn(ExperimentalFaktApi::class, BetaFaktApi::class)
@Tags("experimental", "beta", "testing")
@LogLevel(level = LogLevel.Level.WARNING)
interface CombinedExperimentalService {
    fun useAllExperimentalFeatures(): String
}
