// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.jvmSingleModule.scenarios.suspend

import com.rsicarelli.fakt.samples.jvmSingleModule.models.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthenticationServiceTest {
    @Test
    fun `GIVEN AuthenticationService fake WHEN accessing properties THEN should return configured values`() {
        // Given
        val testUser = User("u1", "Alice", "alice@example.com", 25)
        val testPermissions = setOf("read", "write", "delete")
        val fake = fakeAuthenticationService {
            isLoggedIn { true }
            currentUser { testUser }
            permissions { testPermissions }
        }

        // When
        val loggedIn = fake.isLoggedIn
        val user = fake.currentUser
        val perms = fake.permissions

        // Then
        assertTrue(loggedIn)
        assertEquals("Alice", user?.name)
        assertEquals(3, perms.size)
        assertTrue(perms.contains("write"))
    }

    @Test
    fun `GIVEN AuthenticationService fake WHEN logging in THEN should return Result with User`() =
        runTest {
            // Given
            val fake = fakeAuthenticationService {
                login { username, password ->
                    if (username == "admin" && password == "secret") {
                        Result.success(User("u1", username, "admin@example.com", 30))
                    } else {
                        Result.failure(Exception("Invalid credentials"))
                    }
                }
            }

            // When
            val successResult = fake.login("admin", "secret")
            val failureResult = fake.login("wrong", "wrong")

            // Then
            assertTrue(successResult.isSuccess)
            assertEquals("admin", successResult.getOrNull()?.name)
            assertTrue(failureResult.isFailure)
        }

    @Test
    fun `GIVEN AuthenticationService fake WHEN logging out THEN should return Result Unit`() =
        runTest {
            // Given
            val fake = fakeAuthenticationService {
                logout { Result.success(Unit) }
            }

            // When
            val result = fake.logout()

            // Then
            assertTrue(result.isSuccess)
        }

    @Test
    fun `GIVEN AuthenticationService fake WHEN refreshing token THEN should return Result String`() =
        runTest {
            // Given
            val fake = fakeAuthenticationService {
                refreshToken { Result.success("new-token-12345") }
            }

            // When
            val result = fake.refreshToken()

            // Then
            assertTrue(result.isSuccess)
            assertEquals("new-token-12345", result.getOrNull())
        }

    @Test
    fun `GIVEN AuthenticationService fake WHEN checking single permission THEN should return boolean`() {
        // Given
        val fake = fakeAuthenticationService {
            hasPermission { perm -> perm in setOf("read", "write") }
        }

        // When
        val hasRead = fake.hasPermission("read")
        val hasDelete = fake.hasPermission("delete")

        // Then
        assertTrue(hasRead)
        assertFalse(hasDelete)
    }

    @Test
    fun `GIVEN AuthenticationService fake WHEN checking any permissions THEN should handle list`() {
        // Given
        val userPermissions = setOf("read", "write")
        val fake = fakeAuthenticationService {
            hasAnyPermissions { perms ->
                perms.any { it in userPermissions }
            }
        }

        // When
        val hasAny1 = fake.hasAnyPermissions(listOf("read", "admin"))
        val hasAny2 = fake.hasAnyPermissions(listOf("delete", "admin"))

        // Then
        assertTrue(hasAny1) // Has "read"
        assertFalse(hasAny2) // Has neither
    }

    @Test
    fun `GIVEN AuthenticationService fake WHEN checking all permissions THEN should handle collection`() {
        // Given
        val userPermissions = setOf("read", "write")
        val fake = fakeAuthenticationService {
            hasAllPermissions { perms ->
                perms.all { it in userPermissions }
            }
        }

        // When
        val hasAll1 = fake.hasAllPermissions(listOf("read", "write"))
        val hasAll2 = fake.hasAllPermissions(listOf("read", "delete"))

        // Then
        assertTrue(hasAll1) // Has both
        assertFalse(hasAll2) // Missing "delete"
    }

    @Test
    fun `GIVEN AuthenticationService fake WHEN using defaults THEN should have sensible defaults`() =
        runTest {
            // Given
            val fake = fakeAuthenticationService()

            // When
            val loggedIn = fake.isLoggedIn
            val user = fake.currentUser
            val perms = fake.permissions
            val hasPermResult = fake.hasPermission("any")

            // Then
            assertFalse(loggedIn) // Default: false
            assertNull(user) // Default: null
            assertTrue(perms.isEmpty()) // Default: empty set
            assertFalse(hasPermResult) // Default: false
            // Note: login() not tested with defaults as it returns Result<User> which can't be auto-constructed
        }
}
