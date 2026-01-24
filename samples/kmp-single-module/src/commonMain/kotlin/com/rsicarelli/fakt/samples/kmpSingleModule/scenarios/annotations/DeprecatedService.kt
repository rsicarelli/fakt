// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import com.rsicarelli.fakt.Fake

/**
 * Test case: @Deprecated annotation with all arguments.
 *
 * The generated FakeDeprecatedServiceImpl should include:
 * ```kotlin
 * @Deprecated(
 *     message = "Use NewService instead",
 *     replaceWith = ReplaceWith("NewService"),
 *     level = DeprecationLevel.WARNING
 * )
 * class FakeDeprecatedServiceImpl : DeprecatedService { ... }
 * ```
 *
 * Tests:
 * - String literal argument (message)
 * - Nested annotation argument (replaceWith = ReplaceWith(...))
 * - Enum constant argument (level = DeprecationLevel.WARNING)
 * - Named arguments rendering
 */
@Fake
@Deprecated(
    message = "Use NewService instead",
    replaceWith = ReplaceWith("NewService"),
    level = DeprecationLevel.WARNING,
)
interface DeprecatedService {
    fun oldMethod(): String
}

/**
 * Test case: @Deprecated with minimal arguments.
 *
 * Tests default argument handling - should only render non-default values.
 */
@Fake
@Deprecated("This service will be removed in version 2.0")
interface MinimalDeprecatedService {
    fun legacyOperation(): Int
}

/**
 * Test case: @Deprecated with ERROR level.
 *
 * NOTE: This interface intentionally does NOT have @Fake because ERROR-level
 * deprecated interfaces cannot generate compilable fakes - the generated fake
 * would implement a deprecated interface which causes compiler errors.
 *
 * This demonstrates a known limitation: interfaces deprecated with
 * DeprecationLevel.ERROR cannot have fakes generated for them.
 */
@Deprecated(
    message = "Completely obsolete",
    level = DeprecationLevel.ERROR,
)
interface ErrorDeprecatedService {
    fun obsoleteMethod(): Boolean
}
