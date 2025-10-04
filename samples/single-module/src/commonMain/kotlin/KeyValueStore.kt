// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * P0.1: Multiple type parameters test interface.
 *
 * Tests generic support for interfaces with multiple type parameters (K, V).
 * This is a common pattern in real-world code (Map, Cache, etc.).
 */
@Fake
interface KeyValueStore<K, V> {
    /**
     * Store a key-value pair.
     */
    fun put(
        key: K,
        value: V,
    )

    /**
     * Retrieve a value by key.
     */
    fun get(key: K): V?

    /**
     * Get all entries as a map.
     */
    fun getAll(): Map<K, V>

    /**
     * Remove a key and return its value.
     */
    fun remove(key: K): V?

    /**
     * Check if key exists.
     */
    fun containsKey(key: K): Boolean
}
