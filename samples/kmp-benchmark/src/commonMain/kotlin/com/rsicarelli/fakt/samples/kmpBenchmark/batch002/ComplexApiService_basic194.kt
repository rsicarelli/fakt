// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch002

import com.rsicarelli.fakt.Fake

@Fake
interface ComplexApiService_basic194 {
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
        onRetry: ((Int, kotlin.Exception) -> Unit)? = null,
    ): Result<TResponse>
}
