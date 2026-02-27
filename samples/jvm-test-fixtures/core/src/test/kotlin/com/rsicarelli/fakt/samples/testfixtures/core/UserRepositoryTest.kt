// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core

import com.rsicarelli.fakt.samples.testfixtures.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {
    @Test
    fun `GIVEN UserRepository fake WHEN finding by ID THEN returns configured user`() {
        val fake = fakeUserRepository {
            findById { id -> User(id, "User-$id") }
        }

        val result = fake.findById("42")

        assertEquals("42", result?.id)
        assertEquals("User-42", result?.name)
    }

    @Test
    fun `GIVEN UserRepository fake WHEN saving user THEN returns saved user`() {
        val fake = fakeUserRepository {
            save { user -> user.copy(id = "saved-${user.id}") }
        }

        val result = fake.save(User("1", "Alice"))

        assertEquals("saved-1", result.id)
    }

    @Test
    fun `GIVEN UserRepository fake with defaults WHEN calling findById THEN returns null`() {
        val fake = fakeUserRepository()

        assertNull(fake.findById("any"))
    }

    @Test
    fun `GIVEN UserRepository fake with defaults WHEN calling findAll THEN returns empty list`() {
        val fake = fakeUserRepository()

        assertTrue(fake.findAll().isEmpty())
    }
}
