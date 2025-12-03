// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch077

import com.rsicarelli.fakt.Fake

@Fake
interface AsyncDataService_propertiesAndMethods7654 {
    suspend fun fetchData(): String

    suspend fun <T> processData(data: T): T

    suspend fun batchProcess(items: List<String>): List<String>
}
