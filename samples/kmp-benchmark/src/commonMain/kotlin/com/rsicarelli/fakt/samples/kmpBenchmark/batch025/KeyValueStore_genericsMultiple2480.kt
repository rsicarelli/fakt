// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch025

import com.rsicarelli.fakt.Fake

@Fake
interface KeyValueStore_genericsMultiple2480<K, V> {
    fun put(
        key: K,
        value: V,
    )

    fun get(key: K): V?

    fun getAll(): Map<K, V>

    fun remove(key: K): V?

    fun containsKey(key: K): Boolean
}
