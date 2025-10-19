// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.auth

import com.rsicarelli.fakt.Fake

/**
 * Authentication provider interface for handling user authentication.
 *
 * In a real app, this would be implemented using:
 * - OAuth 2.0 / OpenID Connect
 * - Firebase Authentication
 * - Custom JWT-based auth
 * - Biometric authentication
 *
 * Example usage:
 * ```kotlin
 * val result = authProvider.login("user@example.com", "password")
 * if (result is AuthResult.Success) {
 *     println("Logged in: ${result.session.userId}")
 * }
 * ```
 */
@Fake
interface AuthProvider {
    /**
     * Authenticate with username/email and password.
     */
    suspend fun login(username: String, password: String): AuthResult

    /**
     * Authenticate with an OAuth token.
     */
    suspend fun loginWithOAuth(provider: OAuthProvider, token: String): AuthResult

    /**
     * Authenticate with biometrics (fingerprint, face recognition).
     */
    suspend fun loginWithBiometrics(): AuthResult

    /**
     * Log out the current user.
     */
    suspend fun logout(): Boolean

    /**
     * Refresh the current authentication session.
     */
    suspend fun refreshSession(): AuthResult

    /**
     * Check if a user is currently authenticated.
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Get the current user session, if authenticated.
     */
    suspend fun getCurrentSession(): AuthSession?
}

/**
 * Result of an authentication operation.
 */
sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()

    data class Failure(val error: AuthError) : AuthResult()
}

/**
 * Represents an active authentication session.
 */
data class AuthSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val userInfo: UserInfo,
)

/**
 * User information from authentication.
 */
data class UserInfo(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
)

/**
 * Authentication error types.
 */
enum class AuthError {
    INVALID_CREDENTIALS,
    NETWORK_ERROR,
    SESSION_EXPIRED,
    BIOMETRICS_NOT_AVAILABLE,
    BIOMETRICS_FAILED,
    UNKNOWN,
}

/**
 * OAuth providers.
 */
enum class OAuthProvider {
    GOOGLE,
    FACEBOOK,
    APPLE,
    GITHUB,
}

/**
 * Token storage interface for securely storing authentication tokens.
 *
 * Handles storage and retrieval of access tokens, refresh tokens, and session data.
 */
@Fake
interface TokenStorage {
    /**
     * Store an access token securely.
     */
    suspend fun storeAccessToken(token: String)

    /**
     * Retrieve the stored access token.
     */
    suspend fun getAccessToken(): String?

    /**
     * Store a refresh token securely.
     */
    suspend fun storeRefreshToken(token: String)

    /**
     * Retrieve the stored refresh token.
     */
    suspend fun getRefreshToken(): String?

    /**
     * Store session data.
     */
    suspend fun storeSession(session: AuthSession)

    /**
     * Retrieve stored session data.
     */
    suspend fun getSession(): AuthSession?

    /**
     * Clear all stored tokens and session data.
     */
    suspend fun clear()

    /**
     * Check if there are stored tokens.
     */
    suspend fun hasStoredTokens(): Boolean
}

/**
 * Authorization interface for checking user permissions and roles.
 *
 * Separate from authentication - handles what users are allowed to do.
 */
@Fake
interface Authorizer {
    /**
     * Check if the current user has a specific permission.
     */
    suspend fun hasPermission(permission: String): Boolean

    /**
     * Check if the current user has a specific role.
     */
    suspend fun hasRole(role: String): Boolean

    /**
     * Get all permissions for the current user.
     */
    suspend fun getUserPermissions(): List<String>

    /**
     * Get all roles for the current user.
     */
    suspend fun getUserRoles(): List<String>

    /**
     * Check if the user can access a specific resource.
     */
    suspend fun canAccess(resourceId: String, action: String): Boolean
}
