// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch039

import com.rsicarelli.fakt.Fake

@Fake
interface TripleStore_genericsMultiple3825<K1, K2, V> {
    fun get(
        key1: K1,
        key2: K2,
    ): V?

    fun put(
        key1: K1,
        key2: K2,
        value: V,
    ): V?

    fun contains(
        key1: K1,
        key2: K2,
    ): Boolean

    fun remove(
        key1: K1,
        key2: K2,
    ): V?
}
