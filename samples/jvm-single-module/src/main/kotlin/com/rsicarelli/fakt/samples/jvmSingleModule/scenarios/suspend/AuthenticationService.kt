// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.jvmSingleModule.scenarios.suspend

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.jvmSingleModule.models.User

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
public interface AuthenticationService {
    public val isLoggedIn: Boolean
    public val currentUser: User?
    public val permissions: Set<String>

    public suspend fun login(
        username: String,
        password: String,
    ): Result<User>

    public suspend fun logout(): Result<Unit>

    public suspend fun refreshToken(): Result<String>

    public fun hasPermission(permission: String): Boolean

    public fun hasAnyPermissions(permissions: List<String>): Boolean

    public fun hasAllPermissions(permissions: Collection<String>): Boolean
}
