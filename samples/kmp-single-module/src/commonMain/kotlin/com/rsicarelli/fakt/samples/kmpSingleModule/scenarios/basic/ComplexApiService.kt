// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.basic

import com.rsicarelli.fakt.Fake

/**
 * P3.2: Complex API with default parameters, method-level generics, and suspend.
 *
 * Tests comprehensive feature combination:
 * - Properties with nullable and non-nullable types
 * - Methods with multiple default parameters
 * - Method-level generics with multiple type parameters (TRequest, TResponse)
 * - Suspend functions with generics and lambdas
 * - Nullable lambda parameters
 * Real-world API client scenario with advanced type safety requirements.
 */
@Fake
interface ComplexApiService {
    val baseUrl: String
    val timeout: Long
    val retryCount: Int?

    fun makeRequest(
        endpoint: String,
        method: String = "GET",
        headers: Map<String, String>? = null,
        body: String? = null,
        timeout: Long = 30000L,
    ): String

    suspend fun makeBatchRequests(
        requests: List<Pair<String, Map<String, String>>>,
        parallel: Boolean = true,
        onProgress: ((Int, Int) -> Unit)? = null,
    ): List<Result<String>>

    fun <T> parseResponse(
        response: String,
        parser: (String) -> T?,
        fallback: T? = null,
    ): T?

    suspend fun <TRequest, TResponse> processWithRetry(
        request: TRequest,
        processor: suspend (TRequest) -> TResponse,
        retryCount: Int = 3,
        onRetry: ((Int, Exception) -> Unit)? = null,
    ): Result<TResponse>
}
