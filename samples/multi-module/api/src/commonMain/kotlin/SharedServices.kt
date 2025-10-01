// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package api.shared

import com.rsicarelli.fakt.Fake

// ============================================================================
// SHARED API INTERFACES - Available across all platforms
// Note: Using simple types that work with current compiler
// ============================================================================

@Fake
interface NetworkService {
    suspend fun get(url: String): String
    suspend fun post(url: String, body: String): String
    suspend fun fetchData(url: String): String
    val baseUrl: String
    val timeout: Long
}

@Fake
interface StorageService {
    suspend fun save(key: String, value: String): Boolean
    suspend fun load(key: String): String?
    suspend fun delete(key: String): Boolean
    suspend fun clear(): Boolean
    val isConnected: Boolean
}

@Fake
interface LoggingService {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String, throwable: Throwable? = null)
    fun error(message: String, throwable: Throwable? = null)
    val logLevel: String
}
