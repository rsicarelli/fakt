// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch014

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpBenchmark.models.User

@Fake
interface AuthenticationService_basic1375 {
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
