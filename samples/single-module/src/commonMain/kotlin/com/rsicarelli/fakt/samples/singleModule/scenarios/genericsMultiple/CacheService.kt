// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.genericsMultiple

import com.rsicarelli.fakt.Fake

/**
 * P3.1: Advanced cache with type constraints and suspend method-level generics.
 *
 * Tests complex generic scenarios combining:
 * - Class-level generics (TKey, TValue)
 * - Method-level type parameters with constraints (where R : TValue)
 * - Suspend functions with generics
 * Common in advanced caching patterns with lazy computation and type constraints.
 */
@Fake
interface CacheService<TKey, TValue> {
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
