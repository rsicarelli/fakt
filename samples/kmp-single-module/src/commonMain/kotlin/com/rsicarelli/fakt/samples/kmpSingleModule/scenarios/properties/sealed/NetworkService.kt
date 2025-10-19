// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.properties.sealed

import com.rsicarelli.fakt.Fake

/**
 * Network service using sealed classes (enum-like algebraic data types).
 *
 * Tests:
 * - Sealed class as property type
 * - Sealed class as method parameter
 * - Sealed class as method return type
 * - Generic sealed classes
 * - Nullable sealed classes
 */
@Fake
interface NetworkService {
    /**
     * Current network state.
     */
    val currentState: NetworkResult<Unit>

    /**
     * Last operation result.
     */
    val lastResult: NetworkResult<String>?

    /**
     * Fetch data from network.
     */
    suspend fun fetchData(url: String): NetworkResult<String>

    /**
     * Handle network result.
     */
    fun handleResult(result: NetworkResult<String>): String

    /**
     * Map network result to different type.
     */
    fun <T, R> mapResult(
        result: NetworkResult<T>,
        transform: (T) -> R,
    ): NetworkResult<R>

    /**
     * Check if result is successful.
     */
    fun isSuccess(result: NetworkResult<*>): Boolean
}

/**
 * Network result sealed class for testing sealed hierarchies.
 * Similar to enums but allows associated data.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(
        val data: T,
    ) : NetworkResult<T>()

    data class Error(
        val message: String,
        val code: Int,
    ) : NetworkResult<Nothing>()

    data object Loading : NetworkResult<Nothing>()

    data object Idle : NetworkResult<Nothing>()
}
