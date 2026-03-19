// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.compat

import com.rsicarelli.fakt.Fake

/** Simple interface with properties and functions. */
@Fake
interface UserRepository {
    val currentUser: String
    fun getUser(id: Long): String
    fun findUsers(query: String, limit: Int): List<String>
    suspend fun fetchUser(id: Long): String
}

/** Generic interface with type parameter. */
@Fake
interface Cache<T> {
    fun get(key: String): T?
    fun put(key: String, value: T)
    fun getAll(): List<T>
}

/** Interface with nullable types and default-compatible signatures. */
@Fake
interface NotificationService {
    val isEnabled: Boolean
    fun send(title: String, body: String?): Boolean
    fun getUnreadCount(): Int
    suspend fun markAsRead(id: Long)
}
