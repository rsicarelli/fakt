// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.basic

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpSingleModule.models.User

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
interface UserRepository {
    val users: List<User>

    fun findById(id: String): User?

    fun save(user: User): User

    fun delete(id: String): Boolean

    fun findByAge(
        minAge: Int,
        maxAge: Int = 100,
    ): List<User>
}
