// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.login

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthError
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthResult
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthSession
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.OAuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.TokenStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage

// ============================================================================
// LOGIN FEATURE - Vertical Slice Architecture
// Domain models, use cases, and repositories for login functionality
// Dependencies: core/auth, core/logger, core/storage, core/analytics
// ============================================================================

/**
 * Domain model for login credentials.
 */
data class LoginCredentials(
    val username: String,
    val password: String,
) {
    fun isValid(): Boolean = username.isNotBlank() && password.length >= 6
}

/**
 * Domain model for logged-in user.
 */
data class LoggedInUser(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
)

/**
 * Login result sealed class.
 */
sealed class LoginResult {
    data class Success(val user: LoggedInUser, val session: AuthSession) : LoginResult()

    data class Failure(val reason: LoginFailureReason) : LoginResult()
}

/**
 * Login failure reasons.
 */
enum class LoginFailureReason {
    INVALID_CREDENTIALS,
    NETWORK_ERROR,
    ACCOUNT_LOCKED,
    EMAIL_NOT_VERIFIED,
    UNKNOWN,
}

/**
 * Login use case - Core business logic for authentication.
 *
 * This use case orchestrates the login flow:
 * 1. Validate credentials
 * 2. Authenticate with AuthProvider
 * 3. Store session in TokenStorage
 * 4. Track analytics event
 * 5. Log the result
 */
@Fake
interface LoginUseCase {
    /**
     * Perform login with email and password.
     */
    suspend fun login(
        credentials: LoginCredentials,
        authProvider: AuthProvider,
        tokenStorage: TokenStorage,
        logger: Logger,
        analytics: Analytics,
    ): LoginResult

    /**
     * Perform login with OAuth provider.
     */
    suspend fun loginWithOAuth(
        provider: OAuthProvider,
        token: String,
        authProvider: AuthProvider,
        tokenStorage: TokenStorage,
        logger: Logger,
        analytics: Analytics,
    ): LoginResult

    /**
     * Check if user can log in (e.g., rate limiting).
     */
    suspend fun canAttemptLogin(): Boolean

    /**
     * Get number of failed login attempts.
     */
    suspend fun getFailedAttempts(): Int
}

/**
 * Login repository - Data layer for login-related operations.
 *
 * Handles persistence of login state, failed attempts tracking, etc.
 */
@Fake
interface LoginRepository {
    /**
     * Save login state to local storage.
     */
    suspend fun saveLoginState(user: LoggedInUser, storage: KeyValueStorage)

    /**
     * Get saved login state from local storage.
     */
    suspend fun getLoginState(storage: KeyValueStorage): LoggedInUser?

    /**
     * Clear login state.
     */
    suspend fun clearLoginState(storage: KeyValueStorage)

    /**
     * Increment failed login attempts counter.
     */
    suspend fun incrementFailedAttempts(storage: KeyValueStorage)

    /**
     * Get number of failed login attempts.
     */
    suspend fun getFailedAttempts(storage: KeyValueStorage): Int

    /**
     * Reset failed login attempts counter.
     */
    suspend fun resetFailedAttempts(storage: KeyValueStorage)

    /**
     * Check if account is locked due to too many failed attempts.
     */
    suspend fun isAccountLocked(storage: KeyValueStorage): Boolean
}

/**
 * Login validator - Validates login input and business rules.
 */
@Fake
interface LoginValidator {
    /**
     * Validate email format.
     */
    fun validateEmail(email: String): ValidationResult

    /**
     * Validate password strength.
     */
    fun validatePassword(password: String): ValidationResult

    /**
     * Validate complete login credentials.
     */
    fun validateCredentials(credentials: LoginCredentials): ValidationResult
}

/**
 * Validation result.
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()

    data class Invalid(val errors: List<String>) : ValidationResult()
}
