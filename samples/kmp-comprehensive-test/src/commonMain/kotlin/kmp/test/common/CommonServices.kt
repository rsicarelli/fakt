// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package kmp.test.common

import com.rsicarelli.fakt.Fake

/**
 * RULE TEST 1: commonMain → commonTest
 * These interfaces should generate fakes in commonTest source set
 */

@Fake
interface CommonDataService {
    val version: String
    suspend fun fetchData(query: String): List<String>
    fun <T> processData(data: T): T
}

@Fake
interface CommonCacheService<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V): V?
    fun size(): Int
}

@Fake
interface CommonEventBus {
    fun <T> publish(event: T)
    fun <T> subscribe(eventType: String, handler: (T) -> Unit)
    suspend fun <T> publishAsync(event: T): Boolean
}

// Test data classes for complex scenarios
data class CommonEvent(
    val id: String,
    val type: String,
    val data: Map<String, Any>
)

data class CommonResult<T>(
    val success: Boolean,
    val data: T?,
    val error: String?
)
