// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.androidtestfixtures.producer

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.androidtestfixtures.producer.models.User

/**
 * A `@Fake` interface declared in an Android library's `main` source set.
 *
 * Fakt generates `fakeUserRepository { }` / `FakeUserRepositoryImpl` / `FakeUserRepositoryConfig`
 * into this module's Android `testFixtures` source set (not `test`), so that OTHER Android modules
 * can consume the fake via `testImplementation(testFixtures(project(":producer")))`.
 */
@Fake
interface UserRepository {
    val users: List<User>

    fun findById(id: String): User?

    fun save(user: User): User

    fun delete(id: String): Boolean
}
