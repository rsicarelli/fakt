// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import com.rsicarelli.fakt.Fake

/**
 * Test case: @Suppress annotation with single string argument.
 *
 * The generated FakeSuppressSingleServiceImpl should include:
 * ```kotlin
 * @Suppress("UNCHECKED_CAST")
 * class FakeSuppressSingleServiceImpl : SuppressSingleService { ... }
 * ```
 *
 * Tests:
 * - Vararg with single value
 * - String literal in vararg position
 */
@Fake
@Suppress("UNCHECKED_CAST")
interface SuppressSingleService {
    fun <T> unsafeCast(value: Any): T
}

/**
 * Test case: @Suppress annotation with multiple string arguments.
 *
 * The generated FakeSuppressMultipleServiceImpl should include:
 * ```kotlin
 * @Suppress("UNCHECKED_CAST", "DEPRECATION", "NOTHING_TO_INLINE")
 * class FakeSuppressMultipleServiceImpl : SuppressMultipleService { ... }
 * ```
 *
 * Tests:
 * - Vararg with multiple values
 * - Array argument rendering
 */
@Fake
@Suppress("UNCHECKED_CAST", "DEPRECATION", "NOTHING_TO_INLINE")
interface SuppressMultipleService {
    @Deprecated("test")
    fun <T> legacyCast(value: Any): T
}
