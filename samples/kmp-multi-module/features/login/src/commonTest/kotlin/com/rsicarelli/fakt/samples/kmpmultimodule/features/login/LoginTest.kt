// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.login

import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.fakeAnalytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthError
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthResult
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthSession
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.UserInfo
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.fakeAuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.fakeTokenStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeKeyValueStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Test timestamp constants (KMP-compatible)
private const val TEST_TIMESTAMP = 1_735_689_600_000L // 2025-01-01 00:00:00 UTC
private const val TEST_EXPIRY = TEST_TIMESTAMP + 3_600_000L // +1 hour

/**
 * Tests for LoginUseCase demonstrating cross-module fake usage.
 */
class LoginUseCaseTest {

    @Test
    fun `GIVEN LoginUseCase WHEN login with valid credentials THEN should return success`() =
        runTest {
            // Given
            val mockSession =
                AuthSession(
                    userId = "user-123",
                    accessToken = "token",
                    refreshToken = "refresh",
                    expiresAt = TEST_EXPIRY,
                    userInfo = UserInfo("user-123", "user@example.com", "John Doe"),
                )

            val authProvider =
                fakeAuthProvider {
                    login { username, password ->
                        if (username == "user@example.com" && password == "password123") {
                            AuthResult.Success(mockSession)
                        } else {
                            AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
                        }
                    }
                }

            var storedSession: AuthSession? = null
            val tokenStorage =
                fakeTokenStorage {
                    storeSession { session ->
                        storedSession = session
                    }
                }

            val loggedMessages = mutableListOf<String>()
            val logger =
                fakeLogger {
                    info { message, _ ->
                        loggedMessages.add(message)
                    }
                }

            val trackedEvents = mutableListOf<String>()
            val analytics =
                fakeAnalytics {
                    track { eventName, _ ->
                        trackedEvents.add(eventName)
                    }
                }

            val useCase =
                fakeLoginUseCase {
                    login { credentials, authProv, tokenStore, log, track ->
                        val result = authProv.login(credentials.username, credentials.password)
                        when (result) {
                            is AuthResult.Success -> {
                                tokenStore.storeSession(result.session)
                                log.info("Login successful")
                                track.track("user_login")
                                LoginResult.Success(
                                    LoggedInUser(
                                        result.session.userId,
                                        result.session.userInfo.email,
                                        result.session.userInfo.displayName,
                                    ),
                                    result.session,
                                )
                            }
                            is AuthResult.Failure -> LoginResult.Failure(LoginFailureReason.INVALID_CREDENTIALS)
                        }
                    }
                }

            // When
            val credentials = LoginCredentials("user@example.com", "password123")
            val result = useCase.login(credentials, authProvider, tokenStorage, logger, analytics)

            // Then
            assertTrue(result is LoginResult.Success)
            assertEquals("user-123", result.user.id)
            assertNotNull(storedSession)
            assertTrue(loggedMessages.contains("Login successful"))
            assertTrue(trackedEvents.contains("user_login"))
        }

    @Test
    fun `GIVEN LoginUseCase WHEN login with invalid credentials THEN should return failure`() =
        runTest {
            // Given
            val authProvider =
                fakeAuthProvider {
                    login { _, _ ->
                        AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
                    }
                }

            val tokenStorage = fakeTokenStorage()
            val logger = fakeLogger()
            val analytics = fakeAnalytics()

            val useCase =
                fakeLoginUseCase {
                    login { credentials, authProv, _, _, _ ->
                        val result = authProv.login(credentials.username, credentials.password)
                        when (result) {
                            is AuthResult.Success -> {
                                LoginResult.Success(
                                    LoggedInUser("", "", ""),
                                    result.session,
                                )
                            }
                            is AuthResult.Failure -> LoginResult.Failure(LoginFailureReason.INVALID_CREDENTIALS)
                        }
                    }
                }

            // When
            val credentials = LoginCredentials("wrong@example.com", "wrongpass")
            val result = useCase.login(credentials, authProvider, tokenStorage, logger, analytics)

            // Then
            assertTrue(result is LoginResult.Failure)
            assertEquals(LoginFailureReason.INVALID_CREDENTIALS, result.reason)
        }

    @Test
    fun `GIVEN LoginUseCase WHEN checking can attempt login THEN should return configured value`() =
        runTest {
            // Given
            val useCase =
                fakeLoginUseCase {
                    canAttemptLogin { true }
                }

            // When
            val canAttempt = useCase.canAttemptLogin()

            // Then
            assertTrue(canAttempt)
        }
}

/**
 * Tests for LoginRepository demonstrating data layer operations.
 */
class LoginRepositoryTest {

    @Test
    fun `GIVEN LoginRepository WHEN saving and retrieving login state THEN should return saved user`() =
        runTest {
            // Given
            val storage = mutableMapOf<String, String>()
            val keyValueStorage =
                fakeKeyValueStorage {
                    putString { key, value ->
                        storage[key] = value
                    }
                    getString { key ->
                        storage[key]
                    }
                }

            val repository =
                fakeLoginRepository {
                    saveLoginState { user, store ->
                        store.putString("user_id", user.id)
                        store.putString("user_email", user.email)
                        store.putString("user_name", user.displayName)
                    }
                    getLoginState { store ->
                        val id = store.getString("user_id")
                        val email = store.getString("user_email")
                        val name = store.getString("user_name")

                        if (id != null && email != null && name != null) {
                            LoggedInUser(id, email, name)
                        } else {
                            null
                        }
                    }
                }

            val user = LoggedInUser("user-123", "user@example.com", "John Doe")

            // When
            repository.saveLoginState(user, keyValueStorage)
            val retrieved = repository.getLoginState(keyValueStorage)

            // Then
            assertNotNull(retrieved)
            assertEquals("user-123", retrieved.id)
            assertEquals("user@example.com", retrieved.email)
        }

    @Test
    fun `GIVEN LoginRepository WHEN incrementing failed attempts THEN should track count correctly`() =
        runTest {
            // Given
            val storage = mutableMapOf<String, Int>()
            val keyValueStorage =
                fakeKeyValueStorage {
                    putInt { key, value ->
                        storage[key] = value
                    }
                    getInt { key ->
                        storage[key]
                    }
                }

            val repository =
                fakeLoginRepository {
                    incrementFailedAttempts { store ->
                        val current = store.getInt("failed_attempts") ?: 0
                        store.putInt("failed_attempts", current + 1)
                    }
                    getFailedAttempts { store ->
                        store.getInt("failed_attempts") ?: 0
                    }
                }

            // When
            repository.incrementFailedAttempts(keyValueStorage)
            repository.incrementFailedAttempts(keyValueStorage)
            repository.incrementFailedAttempts(keyValueStorage)
            val attempts = repository.getFailedAttempts(keyValueStorage)

            // Then
            assertEquals(3, attempts)
        }
}

/**
 * Tests for LoginValidator.
 */
class LoginValidatorTest {

    @Test
    fun `GIVEN LoginValidator WHEN validating valid email THEN should return Valid`() {
        // Given
        val validator =
            fakeLoginValidator {
                validateEmail { email ->
                    if (email.contains("@") && email.contains(".")) {
                        ValidationResult.Valid
                    } else {
                        ValidationResult.Invalid(listOf("Invalid email format"))
                    }
                }
            }

        // When
        val result = validator.validateEmail("user@example.com")

        // Then
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `GIVEN LoginValidator WHEN validating invalid email THEN should return Invalid`() {
        // Given
        val validator =
            fakeLoginValidator {
                validateEmail { email ->
                    if (email.contains("@") && email.contains(".")) {
                        ValidationResult.Valid
                    } else {
                        ValidationResult.Invalid(listOf("Invalid email format"))
                    }
                }
            }

        // When
        val result = validator.validateEmail("invalid-email")

        // Then
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `GIVEN LoginValidator WHEN validating weak password THEN should return Invalid`() {
        // Given
        val validator =
            fakeLoginValidator {
                validatePassword { password ->
                    if (password.length >= 8) {
                        ValidationResult.Valid
                    } else {
                        ValidationResult.Invalid(listOf("Password must be at least 8 characters"))
                    }
                }
            }

        // When
        val result = validator.validatePassword("weak")

        // Then
        assertTrue(result is ValidationResult.Invalid)
    }
}
