// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.mutability

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.MutabilityMode
import com.rsicarelli.fakt.samples.kmpSingleModule.models.User

/**
 * Interface with mutable fake enabled.
 *
 * When `mutability = MutabilityMode.MUTABLE`, the generated fake will:
 * - Have `@Volatile private var` behavior properties (instead of `private val`)
 * - Have a `modify {}` method for selective reconfiguration mid-test
 * - Allow changing behavior after construction
 *
 * Use case: Integration tests where mock behavior needs to change during the test
 * (e.g., simulating failure after success, toggling feature flags, etc.)
 */
@Fake(mutability = MutabilityMode.MUTABLE)
interface MutableRepository {
    fun findById(id: String): User?

    suspend fun save(user: User): User

    fun delete(id: String): Boolean

    val status: String
}

/**
 * Interface with immutable fake explicitly set.
 *
 * When `mutability = MutabilityMode.IMMUTABLE`, the generated fake will:
 * - Have `private val` behavior properties (standard, immutable)
 * - NOT have a `modify {}` method
 * - Behavior is fixed at construction time
 *
 * Use case: Override plugin-level default for interfaces that should never be mutable.
 */
@Fake(mutability = MutabilityMode.IMMUTABLE)
interface ImmutableService {
    fun process(value: String): String
}

/**
 * Interface with DEFAULT mutability mode.
 *
 * When `mutability = MutabilityMode.DEFAULT` (or not specified),
 * the generated fake follows the plugin-level default from:
 * `fakt { enableMutableFakes.set(true/false) }`
 *
 * By default, the plugin has mutable fakes DISABLED (immutable).
 */
@Fake
interface DefaultMutabilityService {
    fun execute(command: String): Boolean
}

/**
 * Mutable fake with call history enabled (combined features).
 *
 * Tests that mutable fakes and call history work together correctly.
 * Both features are independent and should compose without conflicts.
 */
@Fake(mutability = MutabilityMode.MUTABLE)
interface MutableTrackedRepository {
    fun find(query: String): List<User>

    fun count(): Int
}
