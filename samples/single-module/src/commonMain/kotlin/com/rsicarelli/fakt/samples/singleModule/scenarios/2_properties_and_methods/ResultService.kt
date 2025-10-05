// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.properties_and_methods

import com.rsicarelli.fakt.Fake

/**
 * P2.3: Method-level generics with Result types.
 *
 * Tests method-level type parameters combined with Kotlin's Result type.
 * Common in error handling patterns where operations can fail and you need to
 * preserve type safety across transformations (tryOperation, mapResult).
 */
@Fake
interface ResultService {
    fun <T> tryOperation(operation: () -> T): Result<T>

    fun <T, R> mapResult(
        result: Result<T>,
        mapper: (T) -> R,
    ): Result<R>

    suspend fun <T> tryAsyncOperation(operation: suspend () -> T): Result<T>

    fun combineResults(results: List<Result<String>>): Result<List<String>>
}
