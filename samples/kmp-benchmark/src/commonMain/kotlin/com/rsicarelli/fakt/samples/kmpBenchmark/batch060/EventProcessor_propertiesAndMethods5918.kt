// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch060

import com.rsicarelli.fakt.Fake

@Fake
interface EventProcessor_propertiesAndMethods5918 {
    fun processString(
        item: String,
        processor: (String) -> String,
    ): String

    fun processInt(
        item: Int,
        processor: (Int) -> String,
    ): String

    fun filter(
        items: List<String>,
        predicate: (String) -> Boolean,
    ): List<String>

    fun onComplete(callback: () -> Unit)

    fun onError(errorHandler: (Exception) -> Unit)

    suspend fun processAsync(
        item: String,
        processor: suspend (String) -> String,
    ): String
}
