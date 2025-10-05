// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package foundation

import com.rsicarelli.fakt.Fake

// ============================================================================
// FOUNDATION MODULE - Low-level utilities
// This is the bottom layer of the dependency chain:
// foundation → domain → features → app
// ============================================================================

/**
 * Logger service - Foundation level logging
 *
 * Tests will validate:
 * - This module generates fakes correctly
 * - Domain module can import and use these fakes
 * - Features module can transitively access these fakes
 * - App module can use fakes from all layers
 */
@Fake
interface Logger {
    fun debug(message: String, tag: String = "App")

    fun info(message: String, tag: String = "App")

    fun warn(message: String, throwable: Throwable? = null, tag: String = "App")

    fun error(message: String, throwable: Throwable? = null, tag: String = "App")

    val minLogLevel: LogLevel
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * Config service - Application configuration
 *
 * Tests cross-module type resolution:
 * - ConfigValue enum must be importable in domain module fakes
 * - Map types must resolve correctly across modules
 */
@Fake
interface ConfigService {
    fun getString(key: String, default: String = ""): String

    fun getInt(key: String, default: Int = 0): Int

    fun getBoolean(key: String, default: Boolean = false): Boolean

    fun getValue(key: String): ConfigValue?

    fun getAllKeys(): List<String>

    val environment: String
}

data class ConfigValue(
    val key: String,
    val value: String,
    val type: String,
)

/**
 * Network service - HTTP client
 *
 * Tests suspend function handling across modules:
 * - Suspend functions must work in generated fakes
 * - Result types must resolve across modules
 * - Complex return types (Result<NetworkResponse>) must compile
 */
@Fake
interface NetworkClient {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): Result<NetworkResponse>

    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Result<NetworkResponse>

    suspend fun put(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Result<NetworkResponse>

    suspend fun delete(url: String, headers: Map<String, String> = emptyMap()): Result<NetworkResponse>

    val baseUrl: String
    val timeout: Long
}

data class NetworkResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String>,
)
