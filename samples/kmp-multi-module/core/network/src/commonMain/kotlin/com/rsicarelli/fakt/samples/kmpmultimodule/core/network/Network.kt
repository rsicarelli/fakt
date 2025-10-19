// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.network

import com.rsicarelli.fakt.Fake

/**
 * HTTP client interface for making network requests.
 *
 * In a real app, this would be implemented using:
 * - Ktor Client
 * - OkHttp
 * - URLSession (iOS native)
 * - Fetch API (JS)
 *
 * Example usage:
 * ```kotlin
 * val response = httpClient.get("https://api.example.com/users")
 * if (response.isSuccessful) {
 *     val users = json.decodeFromString<List<User>>(response.body)
 * }
 * ```
 */
@Fake
interface HttpClient {
    /**
     * Perform a GET request.
     */
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * Perform a POST request.
     */
    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * Perform a PUT request.
     */
    suspend fun put(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * Perform a DELETE request.
     */
    suspend fun delete(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * Perform a PATCH request.
     */
    suspend fun patch(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * The base URL for all requests (e.g., "https://api.example.com").
     */
    val baseUrl: String

    /**
     * Default timeout for requests in milliseconds.
     */
    val timeoutMillis: Long
}

/**
 * Represents an HTTP response.
 */
data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String>,
) {
    val isSuccessful: Boolean
        get() = statusCode in 200..299

    val isClientError: Boolean
        get() = statusCode in 400..499

    val isServerError: Boolean
        get() = statusCode in 500..599
}

/**
 * Higher-level API client interface that wraps HttpClient with typed requests/responses.
 *
 * This represents the application's API client that handles serialization/deserialization,
 * authentication headers, error handling, etc.
 */
@Fake
interface ApiClient {
    /**
     * Perform a typed GET request that returns a result.
     */
    suspend fun <T> get(
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
    ): ApiResult<T>

    /**
     * Perform a typed POST request with a request body.
     */
    suspend fun <T, R> post(
        endpoint: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
    ): ApiResult<R>

    /**
     * Perform a typed PUT request with a request body.
     */
    suspend fun <T, R> put(
        endpoint: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
    ): ApiResult<R>

    /**
     * Perform a typed DELETE request.
     */
    suspend fun <T> delete(
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
    ): ApiResult<T>

    /**
     * Whether the client is currently authenticated.
     */
    val isAuthenticated: Boolean

    /**
     * The current API version being used.
     */
    val apiVersion: String
}

/**
 * Result type for API operations.
 * Represents either success with data or failure with error.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()

    data class Error(
        val code: Int,
        val message: String,
        val throwable: Throwable? = null,
    ) : ApiResult<Nothing>()

    fun isSuccess(): Boolean = this is Success

    fun isError(): Boolean = this is Error

    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            is Error -> null
        }
}

/**
 * WebSocket client interface for real-time bidirectional communication.
 *
 * Used for features like live chat, real-time notifications, collaborative editing, etc.
 */
@Fake
interface WebSocketClient {
    /**
     * Connect to a WebSocket endpoint.
     */
    suspend fun connect(url: String): WebSocketConnection

    /**
     * Disconnect from the current WebSocket connection.
     */
    suspend fun disconnect()

    /**
     * Check if currently connected.
     */
    val isConnected: Boolean
}

/**
 * Represents an active WebSocket connection.
 */
@Fake
interface WebSocketConnection {
    /**
     * Send a text message through the connection.
     */
    suspend fun send(message: String)

    /**
     * Receive messages from the connection.
     * In a real implementation, this would likely return a Flow<String>.
     */
    suspend fun receive(): String?

    /**
     * Close this connection.
     */
    suspend fun close()

    /**
     * Whether this connection is currently open.
     */
    val isOpen: Boolean
}
