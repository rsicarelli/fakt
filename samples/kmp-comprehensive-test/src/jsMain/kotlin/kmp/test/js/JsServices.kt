// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package kmp.test.js

import com.rsicarelli.fakt.Fake

/**
 * RULE TEST 6: jsMain → jsTest
 * These interfaces should generate fakes in jsTest source set
 * FALLBACK TEST: If jsTest doesn't exist, should fallback to commonTest
 */

@Fake
interface JsBrowserService {
    val userAgent: String
    val url: String

    fun navigate(url: String)
    suspend fun fetch(url: String, options: Map<String, Any>): String
    fun <T> evaluateScript(script: String): T?
    fun addEventListener(event: String, handler: (Any) -> Unit)
}

@Fake
interface JsLocalStorageService<T> {
    val storageType: String
    val maxSize: Long

    fun store(key: String, value: T): Boolean
    fun retrieve(key: String): T?
    fun remove(key: String): Boolean
    fun <R> withStorage(action: () -> R): R
}

@Fake
interface JsWebSocketService {
    val isConnected: Boolean
    val url: String

    suspend fun connect(url: String): Boolean
    suspend fun send(message: String): Boolean
    fun disconnect()
    fun <T> onMessage(handler: (String) -> T)
    suspend fun <R> withConnection(action: suspend () -> R): R?
}

// JS-specific data classes
data class JsBrowserInfo(
    val name: String,
    val version: String,
    val platform: String,
    val cookiesEnabled: Boolean
)

data class JsFetchOptions(
    val method: String,
    val headers: Map<String, String>,
    val body: String?,
    val credentials: String?
)

data class JsWebSocketMessage(
    val type: String,
    val data: String,
    val timestamp: Long
)
