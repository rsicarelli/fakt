// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package kmp.test.jvm

import com.rsicarelli.fakt.Fake

/**
 * RULE TEST 2: jvmMain → jvmTest
 * These interfaces should generate fakes in jvmTest source set
 */

@Fake
interface JvmDatabaseService {
    val connectionString: String
    val isConnected: Boolean

    suspend fun connect(): Boolean

    suspend fun disconnect()

    suspend fun executeQuery(sql: String): List<Map<String, Any>>

    fun <T> transaction(block: () -> T): T
}

@Fake
interface JvmFileService {
    fun readFile(path: String): String?

    fun writeFile(
        path: String,
        content: String,
    ): Boolean

    suspend fun copyFile(
        source: String,
        destination: String,
    ): Boolean

    fun <T> withLock(
        file: String,
        action: () -> T,
    ): T
}

@Fake
interface JvmHttpClient<TRequest, TResponse> {
    val baseUrl: String
    val timeout: Long

    suspend fun get(endpoint: String): TResponse?

    suspend fun post(
        endpoint: String,
        body: TRequest,
    ): TResponse?

    suspend fun <R> processResponse(
        response: TResponse,
        processor: (TResponse) -> R,
    ): R

    fun configure(block: (JvmHttpClient<TRequest, TResponse>) -> Unit)
}

// JVM-specific data classes
data class JvmConnection(
    val host: String,
    val port: Int,
    val database: String,
    val credentials: Map<String, String>,
)

data class JvmHttpRequest(
    val method: String,
    val headers: Map<String, String>,
    val body: String?,
)

data class JvmHttpResponse(
    val status: Int,
    val headers: Map<String, String>,
    val body: String,
)
