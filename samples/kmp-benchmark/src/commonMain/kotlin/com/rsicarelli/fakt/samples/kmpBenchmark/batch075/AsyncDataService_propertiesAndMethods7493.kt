// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch075

import com.rsicarelli.fakt.Fake

@Fake
interface AsyncDataService_propertiesAndMethods7493 {
    suspend fun fetchData(): String

    suspend fun <T> processData(data: T): T

    suspend fun batchProcess(items: List<String>): List<String>
}
