// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package api.js

import com.rsicarelli.fakt.Fake

// ============================================================================
// JS-SPECIFIC INTERFACES - JavaScript platform specific features
// Note: Simplified for current compiler capabilities
// ============================================================================

@Fake
interface BrowserService {
    val userAgent: String
    val currentUrl: String

    fun navigateTo(url: String)
    fun reload()
    fun showAlert(message: String)
    fun confirmDialog(message: String): Boolean
    fun promptInput(message: String, defaultValue: String): String?
}

@Fake
interface LocalStorage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
    fun clear()
    fun getLength(): Int
}

@Fake
interface FetchService {
    suspend fun get(url: String): String
    suspend fun post(url: String, body: String): String
    suspend fun put(url: String, body: String): String
    suspend fun delete(url: String): String
}
