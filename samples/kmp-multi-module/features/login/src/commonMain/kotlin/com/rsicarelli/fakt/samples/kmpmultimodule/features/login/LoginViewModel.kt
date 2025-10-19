// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.login

import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.OAuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.TokenStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Login screen state.
 */
sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data class Success(val user: LoggedInUser) : LoginState()
    data class Error(val reason: LoginFailureReason, val message: String) : LoginState()
    data class AccountLocked(val attemptsRemaining: Int = 0) : LoginState()
}

/**
 * Vanilla ViewModel for Login feature (no Android dependencies).
 *
 * Demonstrates production-ready patterns:
 * - StateFlow for reactive state management
 * - K2.2+ backing fields pattern (get() = _field)
 * - Thread-safe state updates with .update { }
 * - Coroutine scope for async operations
 * - Failed attempts tracking with account locking
 * - Support for regular login + OAuth
 *
 * This serves as a real-world example for testing with Fakt + Turbine.
 *
 * NOTE: Call counts and failed attempts are automatically tracked by Fakt fakes!
 * Use `loginUseCase.loginCallCount` and `repository.getFailedAttemptsCallCount` in tests.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val repository: LoginRepository,
    private val authProvider: AuthProvider,
    private val tokenStorage: TokenStorage,
    private val storage: KeyValueStorage,
    private val logger: Logger,
    private val analytics: Analytics,
    private val scope: CoroutineScope,
) {
    // State - K2.2+ backing field pattern
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState>
        get() = _state

    // Failed attempts - K2.2+ backing field pattern
    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int>
        get() = _failedAttempts

    companion object {
        private const val MAX_FAILED_ATTEMPTS = 3
    }

    /**
     * Perform login with credentials.
     * Checks account lock status before attempting login.
     */
    fun login(credentials: LoginCredentials) {
        scope.launch {
            try {
                // Check if account is locked
                val isLocked = repository.isAccountLocked(storage)
                if (isLocked) {
                    _state.update { LoginState.AccountLocked(attemptsRemaining = 0) }
                    logger.warn("Login attempt blocked: account is locked")
                    analytics.track("login_blocked_locked_account")
                    return@launch
                }

                _state.update { LoginState.Loading }
                logger.info("Attempting login for user: ${credentials.username}")

                val result = loginUseCase.login(
                    credentials = credentials,
                    authProvider = authProvider,
                    tokenStorage = tokenStorage,
                    logger = logger,
                    analytics = analytics,
                )

                when (result) {
                    is LoginResult.Success -> {
                        _state.update { LoginState.Success(result.user) }
                        _failedAttempts.update { 0 }
                        repository.resetFailedAttempts(storage)

                        logger.info("Login successful for user: ${result.user.email}")
                        analytics.track("login_success", mapOf("method" to "credentials"))
                    }

                    is LoginResult.Failure -> {
                        handleLoginFailure(result.reason)
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    LoginState.Error(
                        reason = LoginFailureReason.UNKNOWN,
                        message = e.message ?: "Unknown error",
                    )
                }
                logger.error("Login failed with exception", e)
                analytics.track("login_failed", mapOf("error" to e.message.orEmpty()))
            }
        }
    }

    /**
     * Perform login with OAuth provider.
     */
    fun loginWithOAuth(provider: OAuthProvider, token: String) {
        scope.launch {
            try {
                _state.update { LoginState.Loading }
                logger.info("Attempting OAuth login with provider: $provider")

                val result = loginUseCase.loginWithOAuth(
                    provider = provider,
                    token = token,
                    authProvider = authProvider,
                    tokenStorage = tokenStorage,
                    logger = logger,
                    analytics = analytics,
                )

                when (result) {
                    is LoginResult.Success -> {
                        _state.update { LoginState.Success(result.user) }
                        _failedAttempts.update { 0 }

                        logger.info("OAuth login successful: ${result.user.email}")
                        analytics.track("login_success", mapOf("method" to "oauth", "provider" to provider.name))
                    }

                    is LoginResult.Failure -> {
                        _state.update {
                            LoginState.Error(
                                reason = result.reason,
                                message = "OAuth login failed: ${result.reason}",
                            )
                        }
                        logger.warn("OAuth login failed: ${result.reason}")
                        analytics.track("login_oauth_failed", mapOf("provider" to provider.name))
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    LoginState.Error(
                        reason = LoginFailureReason.UNKNOWN,
                        message = e.message ?: "Unknown error",
                    )
                }
                logger.error("OAuth login failed with exception", e)
            }
        }
    }

    /**
     * Retry last login attempt (only if in Error state).
     */
    fun retry() {
        scope.launch {
            val currentState = _state.value
            if (currentState is LoginState.Error) {
                _state.update { LoginState.Idle }
                logger.info("Login retry initiated")
            }
        }
    }

    /**
     * Load failed attempts count from storage.
     */
    fun loadFailedAttempts() {
        scope.launch {
            try {
                val attempts = repository.getFailedAttempts(storage)
                _failedAttempts.update { attempts }
            } catch (e: Exception) {
                logger.error("Failed to load failed attempts", e)
            }
        }
    }

    /**
     * Handle login failure by incrementing failed attempts and checking for account lock.
     */
    private suspend fun handleLoginFailure(reason: LoginFailureReason) {
        repository.incrementFailedAttempts(storage)
        val attempts = repository.getFailedAttempts(storage)
        _failedAttempts.update { attempts }

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            _state.update { LoginState.AccountLocked(attemptsRemaining = 0) }
            logger.warn("Account locked after $attempts failed attempts")
            analytics.track("login_account_locked", mapOf("attempts" to attempts.toString()))
        } else {
            val remaining = MAX_FAILED_ATTEMPTS - attempts
            _state.update {
                LoginState.Error(
                    reason = reason,
                    message = "Login failed: $reason. $remaining attempts remaining.",
                )
            }
            logger.warn("Login failed: $reason. Attempts: $attempts/$MAX_FAILED_ATTEMPTS")
            analytics.track("login_failed", mapOf("reason" to reason.name, "attempts" to attempts.toString()))
        }
    }
}
