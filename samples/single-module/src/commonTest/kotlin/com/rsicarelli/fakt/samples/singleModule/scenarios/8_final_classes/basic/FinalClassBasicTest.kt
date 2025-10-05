// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses

import com.rsicarelli.fakt.samples.singleModule.models.User
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for final class fake generation - basic scenarios.
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinalClassBasicTest {
    @Test
    fun `GIVEN final class fake WHEN configuring open method THEN should use configured behavior`() {
        // Given - create fake with configured behavior
        val testUser = User("test-123", "Test User", "test@example.com")
        val fake =
            fakeUserService {
                getUser { id -> testUser }
            }

        // When
        val result = fake.getUser("any-id")

        // Then
        assertEquals(testUser, result)
        assertEquals("Test User", result.name)
    }

    @Test
    fun `GIVEN final class fake WHEN not configured THEN should use super implementation`() {
        // Given - create fake WITHOUT configuration
        val fake = fakeUserService {}

        // When - call method without configuring behavior
        val result = fake.getUser("some-id")

        // Then - should call super (original implementation)
        assertEquals("some-id", result.id)
        assertEquals("John Doe", result.name) // Default from UserService
    }

    @Test
    fun `GIVEN final class fake WHEN configuring multiple methods THEN should use respective behaviors`() {
        // Given
        var saveCalled = false
        var deleteCalled = false

        val fake =
            fakeUserService {
                getUser { id -> User(id, "Configured User", "configured@test.com") }
                saveUser { user -> saveCalled = true }
                deleteUser { id ->
                    deleteCalled = true
                    false
                }
            }

        // When
        val user = fake.getUser("123")
        fake.saveUser(user)
        val deleted = fake.deleteUser("123")

        // Then
        assertEquals("Configured User", user.name)
        assertTrue(saveCalled, "saveUser should have been called")
        assertTrue(deleteCalled, "deleteUser should have been called")
        assertFalse(deleted, "deleteUser should return false as configured")
    }

    @Test
    fun `GIVEN final class fake WHEN calling final method THEN should use original implementation`() {
        // Given
        val fake = fakeUserService {}

        // When - call final method (not overridable)
        val isValid = fake.validateUserId("test-123")

        // Then - should use original implementation from UserService
        assertTrue(isValid)

        // Empty ID should be invalid
        assertFalse(fake.validateUserId(""))
    }
}
