// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.inheritance

import com.rsicarelli.fakt.samples.kmpSingleModule.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for P1 Scenario: ClassImplementingInterface
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
class UserServiceImplTest {
    @Test
    fun `GIVEN class implementing interface WHEN interface methods not configured THEN should throw error`() {
        // Given
        val service =
            fakeUserServiceImpl {
                // Not configuring interface methods
            }

        // When/Then - interface methods require configuration
        assertFailsWith<IllegalStateException> {
            service.getUser("123")
        }
        assertFailsWith<IllegalStateException> {
            service.saveUser(User("1", "Test"))
        }
    }

    @Test
    fun `GIVEN class implementing interface WHEN interface methods configured THEN should use configured behavior`() {
        // Given
        val testUser = User("test-id", "Test User", "test@example.com")
        val service =
            fakeUserServiceImpl {
                getUser { id -> testUser }
                saveUser { user -> Unit }
            }

        // When
        val result = service.getUser("any-id")

        // Then
        assertEquals(testUser, result)
    }

    @Test
    fun `GIVEN class implementing interface WHEN own methods not configured THEN should use super implementation`() {
        // Given
        val validUser = User("1", "Valid", "valid@test.com")
        val invalidUser = User("", "", "")

        val service =
            fakeUserServiceImpl {
                getUser { User("1", "Test") }
                saveUser { }
                // Not configuring own methods
            }

        // When
        val isValid = service.validateUser(validUser)
        val isInvalid = service.validateUser(invalidUser)
        service.logOperation("test") // Uses super (just prints)

        // Then - uses super implementation
        assertTrue(isValid, "valid user should pass super validation")
        assertFalse(isInvalid, "invalid user should fail super validation")
    }

    @Test
    fun `GIVEN class implementing interface WHEN own methods configured THEN should use configured behavior`() {
        // Given
        var logCalled = false
        val service =
            fakeUserServiceImpl {
                getUser { User("1", "Test") }
                saveUser { }
                validateUser { user -> user.id == "special" }
                logOperation { op -> logCalled = true }
            }

        // When
        val isSpecial = service.validateUser(User("special", "User"))
        val isNormal = service.validateUser(User("normal", "User"))
        service.logOperation("test")

        // Then
        assertTrue(isSpecial, "special user should be valid")
        assertFalse(isNormal, "normal user should be invalid")
        assertTrue(logCalled, "log should have been called")
    }

    @Test
    fun `GIVEN class implementing interface WHEN mixing interface and class methods THEN should distinguish correctly`() {
        // Given
        val service =
            fakeUserServiceImpl {
                // Configure interface methods (required)
                getUser { id -> User(id, "User-$id") }
                saveUser { }
                // Configure one own method
                validateUser { true } // Always valid
                // logOperation uses super
            }

        // When
        val user = service.getUser("123")
        val isValid = service.validateUser(User("", ""))
        service.logOperation("test")

        // Then
        assertEquals("User-123", user.name)
        assertTrue(isValid, "configured to always return true")
    }
}
