// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt

/**
 * Primary annotation for marking interfaces/classes for fake generation.
 *
 * This annotation enables compile-time generation of thread-safe fake implementations that can be
 * used in tests. The generated fakes follow the factory function pattern to ensure instance
 * isolation and eliminate race conditions.
 *
 * ## Basic Usage
 *
 * ```kotlin
 * @Fake
 * interface UserService {
 *     suspend fun getUser(id: String): User
 * }
 *
 * // Usage in tests
 * val userService = fakeUserService {
 *     getUser { id -> User(id, "Test User") }
 * }
 * ```
 *
 * ## Thread Safety
 * Generated fakes are thread-safe by default through instance-based design. Each call to the
 * factory function creates a new isolated instance.
 *
 * ## Call History Control
 * By default, generated fakes include call history tracking for verification. You can control this
 * per-interface using [callHistory]:
 * ```kotlin
 * // Disable call history for lightweight fakes
 * @Fake(callHistory = CallHistoryMode.DISABLED)
 * interface Logger { ... }
 *
 * // Enable call history even if plugin default is disabled
 * @Fake(callHistory = CallHistoryMode.ENABLED)
 * interface PaymentService { ... }
 * ```
 *
 * @property callHistory Controls call history generation for this fake. Defaults to
 *   [CallHistoryMode.DEFAULT] which follows the plugin configuration.
 * @property mutability Controls mutability of the generated fake. Defaults to
 *   [MutabilityMode.DEFAULT] which follows the plugin configuration.
 * @see CallHistoryMode
 * @see MutabilityMode
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Fake(
    val callHistory: CallHistoryMode = CallHistoryMode.DEFAULT,
    val mutability: MutabilityMode = MutabilityMode.DEFAULT,
)
