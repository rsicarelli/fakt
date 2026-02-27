// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt

/**
 * Controls mutability of generated fake implementations.
 *
 * By default, generated fakes are immutable — behaviors are set at construction time via the
 * factory function and cannot be changed afterwards. Mutable fakes allow reconfiguring behaviors
 * mid-test via a `modify {}` method, which is useful for integration tests where the same fake
 * needs to simulate different states during a single test.
 *
 * ## Usage
 *
 * ```kotlin
 * // Follow plugin default (recommended for most cases)
 * @Fake
 * interface UserService { ... }
 *
 * // Always generate mutable fake (for integration tests)
 * @Fake(mutability = MutabilityMode.MUTABLE)
 * interface UserRepository { ... }
 *
 * // Explicitly immutable (override plugin default if set to mutable)
 * @Fake(mutability = MutabilityMode.IMMUTABLE)
 * interface Logger { ... }
 * ```
 *
 * ## Mutable Fake Usage
 *
 * ```kotlin
 * val repo = fakeUserRepository {
 *     findById { id -> User(id, "Alice") }
 * }
 *
 * // Later in the test, reconfigure behavior:
 * repo.modify {
 *     findById { null } // Now returns null
 * }
 * ```
 *
 * @see Fake
 */
public enum class MutabilityMode {
    /**
     * Use the plugin-level default configuration.
     *
     * This is the default behavior when `mutability` is not specified. The plugin default can be
     * configured in `build.gradle.kts`:
     * ```kotlin
     * fakt {
     *     enableMutableFakes.set(false)  // or true
     * }
     * ```
     */
    DEFAULT,

    /**
     * Always generate immutable fakes, regardless of plugin default.
     *
     * Use this when:
     * - Behaviors should not change after construction
     * - Thread safety is critical
     * - Tests are simple and don't require mid-test reconfiguration
     *
     * Generated fakes have `private val` behavior properties set at construction time.
     */
    IMMUTABLE,

    /**
     * Always generate mutable fakes, regardless of plugin default.
     *
     * Use this when:
     * - Integration tests need to reconfigure fakes mid-test
     * - Simulating state changes (e.g., database failures after success)
     * - Testing multiple scenarios with the same injected fake instance
     *
     * Generated fakes have `@Volatile private var` behavior properties and a `modify {}` method
     * that allows selective reconfiguration of individual behaviors.
     */
    MUTABLE,
}
