// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake

import kotlin.reflect.KClass

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
 *
 * @param trackCalls Enables call tracking and verification methods.
 * When true, generates data classes for capturing method calls and verification methods.
 * Performance impact: ~5-10% overhead for call storage.
 * Memory impact: Stores all method calls until cleared.
 *
 * @param builder Generates builder pattern for data classes.
 * When true, creates a builder class with fluent configuration methods.
 * Only applicable to data classes, ignored for interfaces.
 *
 * @param dependencies Auto-inject fake implementations for specified dependencies.
 * Creates instances of specified fakes and provides configuration access.
 * Dependencies must also have @Fake annotations.
 * Cross-module dependencies require proper test dependencies in build.gradle.
 *
 * @param concurrent Ensures thread-safe implementation (enabled by default).
 * When true, generates instance-based fakes instead of singleton objects.
 * When false, allows shared state (NOT RECOMMENDED - race conditions possible).
 *
 * @param scope Scope configuration for fake lifetime management.
 * Supported values: "test" (default), "class", "global".
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Fake(
    val trackCalls: Boolean = false,
    val builder: Boolean = false,
    val dependencies: Array<KClass<*>> = [],
    val concurrent: Boolean = true,
    val scope: String = "test"
)
