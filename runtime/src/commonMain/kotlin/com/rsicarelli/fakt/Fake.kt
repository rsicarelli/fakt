// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt

/**
 * Primary annotation for marking interfaces/classes for fake generation.
 *
 * This annotation enables compile-time generation of thread-safe fake implementations
 * that can be used in tests. The generated fakes follow the factory function pattern
 * to ensure instance isolation and eliminate race conditions.
 *
 * ## Basic Usage
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
 * Generated fakes are thread-safe by default through instance-based design.
 * Each call to the factory function creates a new isolated instance.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Fake
