// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsMultiple

import com.rsicarelli.fakt.Fake

/**
 * P0.1: Multiple type parameters test interface.
 *
 * Tests generic support for interfaces with multiple type parameters (K, V).
 * This is a common pattern in real-world code (Map, Cache, etc.).
 */
@Fake
interface KeyValueStore<K, V> {
    fun put(
        key: K,
        value: V,
    )

    fun get(key: K): V?

    fun getAll(): Map<K, V>

    fun remove(key: K): V?

    fun containsKey(key: K): Boolean
}
