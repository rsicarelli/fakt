// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.testfixtures.core.models.User

/**
 * Repository interface for user management.
 *
 * Demonstrates @Fake on a standard interface with CRUD operations.
 * Generated fakes are placed in testFixtures for cross-module consumption.
 */
@Fake
public interface UserRepository {
    public fun findById(id: String): User?

    public fun save(user: User): User

    public fun delete(id: String): Boolean

    public fun findAll(): List<User>
}
