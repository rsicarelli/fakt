// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.propertiesAndMethods

import com.rsicarelli.fakt.Fake

/**
 * P2.5: Suspend functions with method-level generics.
 *
 * Tests combination of suspend functions with method-level type parameters.
 * Common in async data processing where you need coroutine support and generic
 * operations (processData, batchProcess).
 */
@Fake
interface AsyncDataService {
    suspend fun fetchData(): String

    suspend fun <T> processData(data: T): T

    suspend fun batchProcess(items: List<String>): List<String>
}
