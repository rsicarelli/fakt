// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.androidSingleModule.scenarios.suspend

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.androidSingleModule.models.User

/**
 * Complex interface: properties + suspend functions + Result types + collections.
 *
 * Tests comprehensive feature combination:
 * - Multiple properties (Boolean, nullable User, Set<String>)
 * - Suspend functions returning Result types
 * - Permission checking with different collection types (List vs Collection)
 * Real-world authentication/authorization service pattern.
 */
@Fake
interface AuthenticationService {
    val isLoggedIn: Boolean
    val currentUser: User?
    val permissions: Set<String>

    suspend fun login(
        username: String,
        password: String,
    ): Result<User>

    suspend fun logout(): Result<Unit>

    suspend fun refreshToken(): Result<String>

    fun hasPermission(permission: String): Boolean

    fun hasAnyPermissions(permissions: List<String>): Boolean

    fun hasAllPermissions(permissions: Collection<String>): Boolean
}
