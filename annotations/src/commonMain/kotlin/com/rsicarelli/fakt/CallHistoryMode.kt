// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt

/**
 * Controls call history and call count generation for fake implementations.
 *
 * Call history tracking enables verification of method invocations in tests,
 * similar to mock verification in traditional mocking frameworks. When enabled,
 * generated fakes include:
 * - Call count properties (e.g., `getMethodNameCallCount`)
 * - Call history storage for argument capture
 * - Verification DSL (`verify { method.wasCalledWith(...) }`)
 *
 * ## Usage
 * ```kotlin
 * // Follow plugin default (recommended for most cases)
 * @Fake
 * interface UserService { ... }
 *
 * // Always generate call history (for migration from mocking frameworks)
 * @Fake(callHistory = CallHistoryMode.ENABLED)
 * interface PaymentService { ... }
 *
 * // Never generate call history (lightweight fakes)
 * @Fake(callHistory = CallHistoryMode.DISABLED)
 * interface Logger { ... }
 * ```
 *
 * @see Fake
 */
public enum class CallHistoryMode {
    /**
     * Use the plugin-level default configuration.
     *
     * This is the default behavior when `callHistory` is not specified.
     * The plugin default can be configured in `build.gradle.kts`:
     * ```kotlin
     * fakt {
     *     enableCallHistory.set(true)  // or false
     * }
     * ```
     */
    DEFAULT,

    /**
     * Always generate call history tracking, regardless of plugin default.
     *
     * Use this when:
     * - Migrating from mocking frameworks that require verification
     * - Testing code that needs call count assertions
     * - Verifying method arguments in integration tests
     *
     * Generated code includes:
     * - `val methodNameCallCount: Int` - Number of times method was called
     * - `_methodNameCalls` - Internal history storage
     * - `verify { ... }` - Verification DSL
     */
    ENABLED,

    /**
     * Never generate call history tracking, regardless of plugin default.
     *
     * Use this when:
     * - Creating lightweight fakes without verification overhead
     * - Reducing generated code size
     * - Fakes are only used for stubbing, not verification
     *
     * Generated fakes will only include behavior configuration,
     * without any call tracking infrastructure.
     */
    DISABLED,
}
