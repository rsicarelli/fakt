// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.basic

import com.rsicarelli.fakt.samples.singleModule.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive test for UserRepository fake generation.
 *
 * Validates:
 * - Property access
 * - Methods returning nullable types
 * - Methods with default parameters
 * - Collection handling
 * - Default behaviors
 */
class UserRepositoryTest {
    @Test
    fun `GIVEN UserRepository fake WHEN accessing users property THEN should return configured list`() {
        // Given
        val testUsers =
            listOf(
                User("1", "Alice", "alice@example.com", 25),
                User("2", "Bob", "bob@example.com", 30),
            )
        val fake =
            fakeUserRepository {
                users { testUsers }
            }

        // When
        val result = fake.users

        // Then
        assertEquals(2, result.size)
        assertEquals("Alice", result[0].name)
        assertEquals("Bob", result[1].name)
    }

    @Test
    fun `GIVEN UserRepository fake WHEN finding by ID THEN should return user or null`() {
        // Given
        val fake =
            fakeUserRepository {
                findById { id ->
                    if (id == "existing") User("existing", "Found User", "found@example.com", 28) else null
                }
            }

        // When
        val existingUser = fake.findById("existing")
        val missingUser = fake.findById("missing")

        // Then
        assertEquals("Found User", existingUser?.name)
        assertNull(missingUser)
    }

    @Test
    fun `GIVEN UserRepository fake WHEN saving user THEN should return saved user`() {
        // Given
        val fake =
            fakeUserRepository {
                save { user ->
                    user.copy(id = "saved-${user.id}")
                }
            }

        // When
        val inputUser = User("123", "Test", "test@example.com", 25)
        val result = fake.save(inputUser)

        // Then
        assertEquals("saved-123", result.id)
        assertEquals("Test", result.name)
        assertEquals(25, result.age)
    }

    @Test
    fun `GIVEN UserRepository fake WHEN deleting THEN should return boolean`() {
        // Given
        val fake =
            fakeUserRepository {
                delete { id -> id == "deletable" }
            }

        // When
        val deletedExisting = fake.delete("deletable")
        val deletedMissing = fake.delete("non-existent")

        // Then
        assertTrue(deletedExisting)
        assertFalse(deletedMissing)
    }

    @Test
    fun `GIVEN UserRepository fake WHEN using findByAge with defaults THEN should handle default parameters`() {
        // Given
        val fake =
            fakeUserRepository {
                findByAge { minAge, maxAge ->
                    listOf(
                        User("1", "Young", "young@example.com", minAge),
                        User("2", "Old", "old@example.com", maxAge),
                    )
                }
            }

        // When - call with both parameters
        val resultBoth = fake.findByAge(20, 30)
        // When - call with only minAge (maxAge defaults to 100)
        val resultDefault = fake.findByAge(25)

        // Then
        assertEquals(20, resultBoth[0].age)
        assertEquals(30, resultBoth[1].age)
        assertEquals(25, resultDefault[0].age)
        assertEquals(100, resultDefault[1].age)
    }

    @Test
    fun `GIVEN UserRepository fake WHEN using defaults THEN should have sensible defaults`() {
        // Given
        val fake = fakeUserRepository()

        // When
        val users = fake.users
        val foundUser = fake.findById("any-id")
        val savedUser = fake.save(User("test", "Test User", "test@example.com", 25))
        val deleted = fake.delete("any-id")
        val byAge = fake.findByAge(20, 30)

        // Then
        assertTrue(users.isEmpty()) // Default: empty list
        assertNull(foundUser) // Default: null
        assertEquals("test", savedUser.id) // Default: identity
        assertFalse(deleted) // Default: false
        assertTrue(byAge.isEmpty()) // Default: empty list
    }
}
