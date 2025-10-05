// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.openClass

import com.rsicarelli.fakt.samples.singleModule.models.User
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for P0 Scenario: OpenClassMultipleMethods
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {
    @Test
    fun `GIVEN open class with multiple methods WHEN not configured THEN should use super implementations`() {
        // Given
        val repository = fakeUserRepository {}

        // When
        val user = repository.findById("123")
        val all = repository.findAll()
        val deleted = repository.delete("456")

        // Then - should use default super implementations
        assertNull(user, "findById should return null by default")
        assertTrue(all.isEmpty(), "findAll should return empty list by default")
        assertFalse(deleted, "delete should return false by default")
    }

    @Test
    fun `GIVEN open class WHEN configuring single method THEN should use configured behavior`() {
        // Given
        val testUser = User("test-1", "Test User", "test@example.com")
        val repository =
            fakeUserRepository {
                findById { id -> testUser }
            }

        // When
        val result = repository.findById("any-id")

        // Then
        assertEquals(testUser, result)
        assertEquals("Test User", result?.name)
    }

    @Test
    fun `GIVEN open class WHEN configuring multiple methods THEN should use respective behaviors`() {
        // Given
        var saveCalled = false
        var deleteCalled = false
        val testUsers =
            listOf(
                User("1", "User 1", "user1@test.com"),
                User("2", "User 2", "user2@test.com"),
            )

        val repository =
            fakeUserRepository {
                save { user -> saveCalled = true }
                delete { id ->
                    deleteCalled = true
                    true
                }
                findAll { testUsers }
            }

        // When
        repository.save(testUsers[0])
        val deleted = repository.delete("1")
        val all = repository.findAll()

        // Then
        assertTrue(saveCalled, "save should have been called")
        assertTrue(deleteCalled, "delete should have been called")
        assertTrue(deleted, "delete should return true as configured")
        assertEquals(2, all.size)
        assertEquals(testUsers, all)
    }

    @Test
    fun `GIVEN open class WHEN partially configured THEN should mix super and configured behaviors`() {
        // Given - only configure findById, others use super
        val testUser = User("configured", "Configured", "config@test.com")
        val repository =
            fakeUserRepository {
                findById { id -> if (id == "configured") testUser else null }
            }

        // When
        val found = repository.findById("configured")
        val notFound = repository.findById("other")
        val all = repository.findAll() // Not configured, should use super

        // Then
        assertEquals(testUser, found)
        assertNull(notFound)
        assertTrue(all.isEmpty(), "findAll should use super (empty list)")
    }
}
