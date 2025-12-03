// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch011

import com.rsicarelli.fakt.Fake

@Fake
interface ResultService_propertiesAndMethods1046 {
    fun <T> tryOperation(operation: () -> T): Result<T>

    fun <T, R> mapResult(
        result: Result<T>,
        mapper: (T) -> R,
    ): Result<R>

    suspend fun <T> tryAsyncOperation(operation: suspend () -> T): Result<T>

    fun combineResults(results: List<Result<String>>): Result<List<String>>
}
