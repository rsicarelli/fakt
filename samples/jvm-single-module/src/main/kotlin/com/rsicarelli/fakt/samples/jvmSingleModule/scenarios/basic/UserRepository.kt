// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.jvmSingleModule.scenarios.basic

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.jvmSingleModule.models.User

/**
 * Interface with properties, methods, and default parameters (domain model usage).
 *
 * Tests typical repository pattern combining:
 * - Collection property (val users: List<User>)
 * - CRUD operations (findById, save, delete)
 * - Method with default parameter (findByAge with maxAge = 100)
 * Validates real-world domain-driven design scenarios with custom data types.
 */
@Fake
public interface UserRepository {
    public val users: List<User>

    public fun findById(id: String): User?

    public fun save(user: User): User

    public fun delete(id: String): Boolean

    public fun findByAge(
        minAge: Int,
        maxAge: Int = 100,
    ): List<User>
}
