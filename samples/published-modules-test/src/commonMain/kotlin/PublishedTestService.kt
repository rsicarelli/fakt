// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.sample.published

import com.rsicarelli.fakt.Fake

@Fake
interface PublishedTestService {
    val name: String
    val isActive: Boolean

    fun getName(): String
    fun setActive(active: Boolean)
    fun processData(input: String): String
}

@Fake
interface PublishedUserService {
    suspend fun fetchUser(id: String): String
    fun hasPermission(permission: String): Boolean
    fun validateCredentials(username: String, password: String): Boolean
}
