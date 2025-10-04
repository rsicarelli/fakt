// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * P0.2: Nested generics test interface.
 *
 * Tests support for nested generic types like List<List<T>>, Map<K, List<V>>, etc.
 * Common in real-world scenarios (batch operations, grouped data, etc.).
 */
@Fake
interface DataCache<T> {
    /**
     * Store a batch of items.
     */
    fun storeBatch(items: List<T>)

    /**
     * Get all batches stored.
     */
    fun getAllBatches(): List<List<T>>

    /**
     * Store grouped data.
     */
    fun storeGroups(groups: Map<String, List<T>>)

    /**
     * Get a specific group.
     */
    fun getGroup(name: String): List<T>?

    /**
     * Store nested structure (list of lists).
     */
    fun storeNested(data: List<List<T>>)

    /**
     * Get matrix-like data structure.
     */
    fun getMatrix(): List<List<T>>
}
