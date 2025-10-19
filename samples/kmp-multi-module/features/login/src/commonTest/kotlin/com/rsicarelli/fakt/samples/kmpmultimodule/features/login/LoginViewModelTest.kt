// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.rsicarelli.fakt.samples.kmpmultimodule.features.login

import app.cash.turbine.test
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.fakeAnalytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthSession
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.OAuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.TokenStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.UserInfo
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.fakeAuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.fakeTokenStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeKeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for LoginViewModel demonstrating:
 * - Turbine for StateFlow testing
 * - K2.2+ backing fields pattern
 * - Account locking logic
 * - Failed attempts tracking (using Fakt!)
 * - OAuth flow
 * - Concurrency and thread safety
 * - Factory pattern for clean test setup
 *
 * This serves as a production-ready example for the Fakt community.
 */
class LoginViewModelTest {

    companion object {
        // Helper to create test AuthSession
        private fun createTestSession(
            userId: String = "test-123",
            email: String = "test@example.com",
            displayName: String = "Test User",
        ) = AuthSession(
            userId = userId,
            accessToken = "test-access-token",
            refreshToken = "test-refresh-token",
            expiresAt = 1735689600000L, // 2025-01-01 00:00:00 UTC
            userInfo = UserInfo(
                id = userId,
                email = email,
                displayName = displayName,
            ),
        )
    }

    // ============================================================================
    // LOGIN SUCCESS TESTS
    // ============================================================================

    @Test
    fun `GIVEN valid credentials WHEN logging in THEN should transition to Success state`() =
        runTest {
            // Given
            val testUser = LoggedInUser(
                id = "123",
                email = "test@example.com",
                displayName = "Test User",
            )

            val loginUseCase = fakeLoginUseCase {
                login { _, _, _, _, _ ->
                    LoginResult.Success(
                        user = testUser,
                        session = createTestSession(
                            userId = testUser.id,
                            email = testUser.email,
                            displayName = testUser.displayName,
                        ),
                    )
                }
            }

            val viewModel = factoryLoginViewModel(loginUseCase = loginUseCase)

            // When
            viewModel.state.test {
                assertEquals(LoginState.Idle, awaitItem())

                viewModel.login(LoginCredentials("test@example.com", "password123"))
                advanceUntilIdle()

                assertEquals(LoginState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is LoginState.Success)
                assertEquals(testUser.email, successState.user.email)
            }
        }

    @Test
    fun `GIVEN successful login WHEN checking failed attempts THEN should reset to zero`() =
        runTest {
            // Given
            val repository = fakeLoginRepository {
                isAccountLocked { false }
                getFailedAttempts { 2 } // Had previous failures
                resetFailedAttempts { }
            }

            val loginUseCase = fakeLoginUseCase {
                login { _, _, _, _, _ ->
                    LoginResult.Success(
                        user = LoggedInUser("1", "test@test.com", "Test"),
                        session = createTestSession(
                            userId = "1",
                            email = "test@test.com",
                            displayName = "Test"
                        ),
                    )
                }
            }

            val viewModel = factoryLoginViewModel(
                loginUseCase = loginUseCase,
                repository = repository,
            )

            // When
            viewModel.login(LoginCredentials("test@test.com", "password"))
            advanceUntilIdle()

            // Then - Fakt tracks the reset call!
            repository.resetFailedAttemptsCallCount.test {
                assertEquals(1, awaitItem())
            }

            viewModel.failedAttempts.test {
                assertEquals(0, awaitItem())
            }
        }

    // ============================================================================
    // LOGIN FAILURE & FAILED ATTEMPTS TESTS
    // ============================================================================

    @Test
    fun `GIVEN invalid credentials WHEN logging in THEN should increment failed attempts`() =
        runTest {
            // Given
            var attemptCount = 0
            val repository = fakeLoginRepository {
                isAccountLocked { false }
                incrementFailedAttempts { attemptCount++ }
                getFailedAttempts { attemptCount }
            }

            val loginUseCase = fakeLoginUseCase {
                login { _, _, _, _, _ ->
                    LoginResult.Failure(LoginFailureReason.INVALID_CREDENTIALS)
                }
            }

            val viewModel = factoryLoginViewModel(
                loginUseCase = loginUseCase,
                repository = repository,
            )

            // When
            viewModel.login(LoginCredentials("wrong@test.com", "wrong"))
            advanceUntilIdle()

            // Then
            repository.incrementFailedAttemptsCallCount.test {
                assertEquals(1, awaitItem())
            }

            viewModel.failedAttempts.test {
                assertEquals(1, awaitItem())
            }

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is LoginState.Error)
                assertEquals(LoginFailureReason.INVALID_CREDENTIALS, state.reason)
            }
        }

    @Test
    fun `GIVEN 3 failed attempts WHEN logging in again THEN should lock account`() =
        runTest {
            // Given
            var attemptCount = 2 // Already 2 failed attempts
            val repository = fakeLoginRepository {
                isAccountLocked { attemptCount >= 3 }
                incrementFailedAttempts { attemptCount++ }
                getFailedAttempts { attemptCount }
            }

            val loginUseCase = fakeLoginUseCase {
                login { _, _, _, _, _ ->
                    LoginResult.Failure(LoginFailureReason.INVALID_CREDENTIALS)
                }
            }

            val viewModel = factoryLoginViewModel(
                loginUseCase = loginUseCase,
                repository = repository
            )

            // When - Third failed attempt
            viewModel.login(LoginCredentials("test@test.com", "wrong"))
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is LoginState.AccountLocked)
                assertEquals(0, state.attemptsRemaining)
            }

            viewModel.failedAttempts.test {
                assertEquals(3, awaitItem())
            }
        }

    @Test
    fun `GIVEN locked account WHEN attempting login THEN should block immediately`() =
        runTest {
            // Given
            val repository = fakeLoginRepository {
                isAccountLocked { true } // Account already locked
            }

            val loginUseCase = fakeLoginUseCase() // Should not be called

            val viewModel = factoryLoginViewModel(
                loginUseCase = loginUseCase,
                repository = repository
            )

            // When
            viewModel.login(LoginCredentials("test@test.com", "password"))
            advanceUntilIdle()

            // Then - Should NOT call login use case
            loginUseCase.loginCallCount.test {
                assertEquals(0, awaitItem())
            }

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is LoginState.AccountLocked)
            }
        }

    // ============================================================================
    // OAUTH LOGIN TESTS
    // ============================================================================

    @Test
    fun `GIVEN OAuth token WHEN logging in with OAuth THEN should succeed`() =
        runTest {
            // Given
            val testUser = LoggedInUser("oauth-123", "oauth@test.com", "OAuth User")

            val loginUseCase = fakeLoginUseCase {
                loginWithOAuth { _, _, _, _, _, _ ->
                    LoginResult.Success(
                        user = testUser,
                        session = createTestSession(
                            userId = testUser.id,
                            email = testUser.email,
                            displayName = testUser.displayName,
                        ),
                    )
                }
            }

            val viewModel = factoryLoginViewModel(loginUseCase = loginUseCase)

            // When
            viewModel.state.test {
                assertEquals(LoginState.Idle, awaitItem())

                viewModel.loginWithOAuth(OAuthProvider.GOOGLE, "oauth-token-123")
                advanceUntilIdle()

                assertEquals(LoginState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is LoginState.Success)
                assertEquals("oauth@test.com", successState.user.email)
            }
        }

    @Test
    fun `GIVEN OAuth failure WHEN logging in with OAuth THEN should show error`() =
        runTest {
            // Given
            val loginUseCase = fakeLoginUseCase {
                loginWithOAuth { _, _, _, _, _, _ ->
                    LoginResult.Failure(LoginFailureReason.NETWORK_ERROR)
                }
            }

            val viewModel = factoryLoginViewModel(loginUseCase = loginUseCase)

            // When
            viewModel.loginWithOAuth(OAuthProvider.GITHUB, "invalid-token")
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is LoginState.Error)
                assertEquals(LoginFailureReason.NETWORK_ERROR, state.reason)
            }
        }

    // ============================================================================
    // RETRY LOGIC TESTS
    // ============================================================================

    @Test
    fun `GIVEN Error state WHEN retrying THEN should reset to Idle`() =
        runTest {
            // Given
            val loginUseCase = fakeLoginUseCase {
                login { _, _, _, _, _ ->
                    LoginResult.Failure(LoginFailureReason.NETWORK_ERROR)
                }
            }

            val repository = fakeLoginRepository {
                isAccountLocked { false }
                incrementFailedAttempts { }
                getFailedAttempts { 1 }
            }

            val viewModel = factoryLoginViewModel(
                loginUseCase = loginUseCase,
                repository = repository,
            )

            // When - Login fails
            viewModel.login(LoginCredentials("test@test.com", "password"))
            advanceUntilIdle()

            // Then retry
            viewModel.state.test {
                assertTrue(awaitItem() is LoginState.Error)

                viewModel.retry()
                advanceUntilIdle()

                assertEquals(LoginState.Idle, awaitItem())
            }
        }

    // ============================================================================
    // LOAD FAILED ATTEMPTS TESTS
    // ============================================================================

    @Test
    fun `GIVEN stored failed attempts WHEN loading THEN should update failedAttempts state`() =
        runTest {
            // Given
            val repository = fakeLoginRepository {
                getFailedAttempts { 2 }
            }

            val viewModel = factoryLoginViewModel(repository = repository)

            // When
            viewModel.loadFailedAttempts()
            advanceUntilIdle()

            // Then
            viewModel.failedAttempts.test {
                assertEquals(2, awaitItem())
            }

            repository.getFailedAttemptsCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    // ============================================================================
    // CONCURRENCY & THREAD SAFETY TESTS
    // ============================================================================

    @Test
    fun `GIVEN LoginViewModel WHEN 10 concurrent logins THEN should be thread safe`() =
        runTest {
            // Given
            val loginUseCase = fakeLoginUseCase {
                login { _, _, _, _, _ ->
                    delay(10)
                    LoginResult.Success(
                        user = LoggedInUser("1", "test@test.com", "Test"),
                        session = createTestSession(
                            userId = "1",
                            email = "test@test.com",
                            displayName = "Test"
                        ),
                    )
                }
            }

            val repository = fakeLoginRepository {
                isAccountLocked { false }
                resetFailedAttempts { }
            }

            val viewModel = factoryLoginViewModel(
                loginUseCase = loginUseCase,
                repository = repository,
            )

            // When - 10 concurrent logins
            repeat(10) {
                launch {
                    viewModel.login(LoginCredentials("test$it@test.com", "password"))
                }
            }
            advanceUntilIdle()

            // Then - Fakt tracks all 10 calls!
            loginUseCase.loginCallCount.test {
                assertEquals(10, awaitItem())
            }
        }

    // ============================================================================
    // ANALYTICS & LOGGER VALIDATION TESTS
    // ============================================================================

    @Test
    fun `GIVEN successful login WHEN tracking analytics THEN should track login_success event`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val loginUseCase = fakeLoginUseCase {
                login { _, _, _, _, _ ->
                    LoginResult.Success(
                        user = LoggedInUser("1", "test@test.com", "Test"),
                        session = createTestSession(
                            userId = "1",
                            email = "test@test.com",
                            displayName = "Test"
                        ),
                    )
                }
            }

            val repository = fakeLoginRepository {
                isAccountLocked { false }
                resetFailedAttempts { }
            }

            val viewModel = factoryLoginViewModel(
                loginUseCase = loginUseCase,
                repository = repository,
                analytics = analytics,
            )

            // When
            viewModel.login(LoginCredentials("test@test.com", "password"))
            advanceUntilIdle()

            // Then
            assertTrue(trackedEvents.contains("login_success"))
        }

    @Test
    fun `GIVEN locked account WHEN attempting login THEN should track login_blocked_locked_account`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val repository = fakeLoginRepository {
                isAccountLocked { true }
            }

            val viewModel = factoryLoginViewModel(
                repository = repository,
                analytics = analytics,
            )

            // When
            viewModel.login(LoginCredentials("test@test.com", "password"))
            advanceUntilIdle()

            // Then
            assertTrue(trackedEvents.contains("login_blocked_locked_account"))
        }

    @Test
    fun `GIVEN login flow WHEN executing THEN should log all messages`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val loginUseCase = fakeLoginUseCase {
                login { _, _, _, _, _ ->
                    LoginResult.Success(
                        user = LoggedInUser("1", "test@test.com", "Test"),
                        session = createTestSession(
                            userId = "1",
                            email = "test@test.com",
                            displayName = "Test"
                        ),
                    )
                }
            }

            val repository = fakeLoginRepository {
                isAccountLocked { false }
                resetFailedAttempts { }
            }

            val viewModel = factoryLoginViewModel(
                loginUseCase = loginUseCase,
                repository = repository,
                logger = logger,
            )

            // When
            viewModel.login(LoginCredentials("test@test.com", "password"))
            advanceUntilIdle()

            // Then
            assertTrue(loggedMessages.any { it.contains("Attempting login") })
            assertTrue(loggedMessages.any { it.contains("Login successful") })
        }

    // ============================================================================
    // HELPER FACTORY
    // ============================================================================

    private fun TestScope.factoryLoginViewModel(
        loginUseCase: LoginUseCase = fakeLoginUseCase(),
        repository: LoginRepository = fakeLoginRepository(),
        authProvider: AuthProvider = fakeAuthProvider(),
        tokenStorage: TokenStorage = fakeTokenStorage(),
        storage: KeyValueStorage = fakeKeyValueStorage(),
        logger: Logger = fakeLogger(),
        analytics: Analytics = fakeAnalytics(),
    ) = LoginViewModel(
        loginUseCase = loginUseCase,
        repository = repository,
        authProvider = authProvider,
        tokenStorage = tokenStorage,
        storage = storage,
        logger = logger,
        analytics = analytics,
        scope = this,
    )
}
