// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch045

import com.rsicarelli.fakt.Fake

@Fake
interface CacheService_genericsMultiple4469<TKey, TValue> {
    val size: Int
    val maxSize: Int?

    fun get(key: TKey): TValue?

    fun put(
        key: TKey,
        value: TValue,
    ): TValue?

    fun remove(key: TKey): TValue?

    fun clear()

    fun containsKey(key: TKey): Boolean

    fun keys(): Set<TKey>

    fun values(): Collection<TValue>

    fun <R> computeIfAbsent(
        key: TKey,
        computer: (TKey) -> R,
    ): R where R : TValue

    suspend fun <R> asyncComputeIfAbsent(
        key: TKey,
        computer: suspend (TKey) -> R,
    ): R where R : TValue
}
