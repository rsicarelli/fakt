// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.genericsBasic

import com.rsicarelli.fakt.Fake

/**
 * P0.2: Nested generics test interface.
 *
 * Tests support for nested generic types like List<List<T>>, Map<K, List<V>>, etc.
 * Common in real-world scenarios (batch operations, grouped data, etc.).
 */
@Fake
interface DataCache<T> {
    fun storeBatch(items: List<T>)

    fun getAllBatches(): List<List<T>>

    fun storeGroups(groups: Map<String, List<T>>)

    fun getGroup(name: String): List<T>?

    fun storeNested(data: List<List<T>>)

    fun getMatrix(): List<List<T>>
}
