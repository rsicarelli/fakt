// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for AuthProvider fake generation and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthProviderTest {

    @Test
    fun `GIVEN AuthProvider fake WHEN logging in with valid credentials THEN should return success`() =
        runTest {
            // Given
            val mockSession =
                AuthSession(
                    userId = "user-123",
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    expiresAt = System.currentTimeMillis() + 3600000,
                    userInfo = UserInfo("user-123", "user@example.com", "John Doe"),
                )

            val authProvider =
                fakeAuthProvider {
                    login { username, password ->
                        if (username == "user@example.com" && password == "correct") {
                            AuthResult.Success(mockSession)
                        } else {
                            AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
                        }
                    }
                }

            // When
            val result = authProvider.login("user@example.com", "correct")

            // Then
            assertTrue(result is AuthResult.Success)
            assertEquals("user-123", (result as AuthResult.Success).session.userId)
        }

    @Test
    fun `GIVEN AuthProvider fake WHEN logging in with invalid credentials THEN should return failure`() =
        runTest {
            // Given
            val authProvider =
                fakeAuthProvider {
                    login { _, _ ->
                        AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
                    }
                }

            // When
            val result = authProvider.login("user@example.com", "wrong")

            // Then
            assertTrue(result is AuthResult.Failure)
            assertEquals(AuthError.INVALID_CREDENTIALS, (result as AuthResult.Failure).error)
        }

    @Test
    fun `GIVEN AuthProvider fake WHEN logging in with OAuth THEN should return success`() =
        runTest {
            // Given
            val mockSession =
                AuthSession(
                    userId = "google-user-123",
                    accessToken = "google-access-token",
                    refreshToken = "google-refresh-token",
                    expiresAt = System.currentTimeMillis() + 3600000,
                    userInfo = UserInfo("google-user-123", "user@gmail.com", "John Doe"),
                )

            val authProvider =
                fakeAuthProvider {
                    loginWithOAuth { provider, _ ->
                        if (provider == OAuthProvider.GOOGLE) {
                            AuthResult.Success(mockSession)
                        } else {
                            AuthResult.Failure(AuthError.UNKNOWN)
                        }
                    }
                }

            // When
            val result = authProvider.loginWithOAuth(OAuthProvider.GOOGLE, "google-token")

            // Then
            assertTrue(result is AuthResult.Success)
        }

    @Test
    fun `GIVEN AuthProvider fake WHEN checking authentication status THEN should return configured value`() =
        runTest {
            // Given
            val authProvider =
                fakeAuthProvider {
                    isAuthenticated { true }
                }

            // When
            val authenticated = authProvider.isAuthenticated()

            // Then
            assertTrue(authenticated)
        }

    @Test
    fun `GIVEN AuthProvider fake WHEN getting current session THEN should return configured session`() =
        runTest {
            // Given
            val mockSession =
                AuthSession(
                    userId = "user-123",
                    accessToken = "token",
                    refreshToken = "refresh",
                    expiresAt = System.currentTimeMillis() + 3600000,
                    userInfo = UserInfo("user-123", "user@example.com", "John Doe"),
                )

            val authProvider =
                fakeAuthProvider {
                    getCurrentSession { mockSession }
                }

            // When
            val session = authProvider.getCurrentSession()

            // Then
            assertNotNull(session)
            assertEquals("user-123", session.userId)
        }
}

/**
 * Tests for TokenStorage fake generation and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenStorageTest {

    @Test
    fun `GIVEN TokenStorage fake WHEN storing and retrieving access token THEN should return stored token`() =
        runTest {
            // Given
            var storedToken: String? = null
            val storage =
                fakeTokenStorage {
                    storeAccessToken { token ->
                        storedToken = token
                    }
                    getAccessToken {
                        storedToken
                    }
                }

            // When
            storage.storeAccessToken("my-access-token")
            val retrieved = storage.getAccessToken()

            // Then
            assertEquals("my-access-token", retrieved)
        }

    @Test
    fun `GIVEN TokenStorage fake WHEN storing and retrieving session THEN should return stored session`() =
        runTest {
            // Given
            var storedSession: AuthSession? = null
            val storage =
                fakeTokenStorage {
                    storeSession { session ->
                        storedSession = session
                    }
                    getSession {
                        storedSession
                    }
                }

            val mockSession =
                AuthSession(
                    userId = "user-123",
                    accessToken = "token",
                    refreshToken = "refresh",
                    expiresAt = System.currentTimeMillis(),
                    userInfo = UserInfo("user-123", "user@example.com", "John"),
                )

            // When
            storage.storeSession(mockSession)
            val retrieved = storage.getSession()

            // Then
            assertNotNull(retrieved)
            assertEquals("user-123", retrieved.userId)
        }

    @Test
    fun `GIVEN TokenStorage fake WHEN clearing storage THEN should remove all tokens`() =
        runTest {
            // Given
            var storedAccessToken: String? = "token"
            var storedRefreshToken: String? = "refresh"

            val storage =
                fakeTokenStorage {
                    storeAccessToken { token ->
                        storedAccessToken = token
                    }
                    getAccessToken {
                        storedAccessToken
                    }
                    storeRefreshToken { token ->
                        storedRefreshToken = token
                    }
                    getRefreshToken {
                        storedRefreshToken
                    }
                    clear {
                        storedAccessToken = null
                        storedRefreshToken = null
                    }
                }

            storage.storeAccessToken("token")
            storage.storeRefreshToken("refresh")

            // When
            storage.clear()

            // Then
            assertNull(storage.getAccessToken())
            assertNull(storage.getRefreshToken())
        }

    @Test
    fun `GIVEN TokenStorage fake WHEN checking for stored tokens THEN should return correct value`() =
        runTest {
            // Given
            val storage =
                fakeTokenStorage {
                    hasStoredTokens { true }
                }

            // When
            val hasTokens = storage.hasStoredTokens()

            // Then
            assertTrue(hasTokens)
        }
}

/**
 * Tests for Authorizer fake generation and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizerTest {

    @Test
    fun `GIVEN Authorizer fake WHEN checking permission THEN should return configured value`() =
        runTest {
            // Given
            val authorizer =
                fakeAuthorizer {
                    hasPermission { permission ->
                        permission == "read:users"
                    }
                }

            // When
            val hasPermission = authorizer.hasPermission("read:users")
            val hasOther = authorizer.hasPermission("write:users")

            // Then
            assertTrue(hasPermission)
            assertFalse(hasOther)
        }

    @Test
    fun `GIVEN Authorizer fake WHEN checking role THEN should return configured value`() =
        runTest {
            // Given
            val authorizer =
                fakeAuthorizer {
                    hasRole { role ->
                        role == "admin"
                    }
                }

            // When
            val isAdmin = authorizer.hasRole("admin")
            val isUser = authorizer.hasRole("user")

            // Then
            assertTrue(isAdmin)
            assertFalse(isUser)
        }

    @Test
    fun `GIVEN Authorizer fake WHEN getting user permissions THEN should return configured list`() =
        runTest {
            // Given
            val authorizer =
                fakeAuthorizer {
                    getUserPermissions {
                        listOf("read:users", "write:users", "delete:users")
                    }
                }

            // When
            val permissions = authorizer.getUserPermissions()

            // Then
            assertEquals(3, permissions.size)
            assertTrue(permissions.contains("read:users"))
        }

    @Test
    fun `GIVEN Authorizer fake WHEN checking resource access THEN should return configured value`() =
        runTest {
            // Given
            val authorizer =
                fakeAuthorizer {
                    canAccess { resourceId, action ->
                        resourceId == "order-123" && action == "read"
                    }
                }

            // When
            val canRead = authorizer.canAccess("order-123", "read")
            val canWrite = authorizer.canAccess("order-123", "write")

            // Then
            assertTrue(canRead)
            assertFalse(canWrite)
        }
}
