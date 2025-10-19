// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.generics

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: MultiParameterGenericClass
 *
 * **Pattern**: Open class with two independent generic type parameters (K, V)
 * **Priority**: P1 (High - Common Cache/Map Pattern)
 *
 * **What it tests**:
 * - Two type parameters (K for key, V for value)
 * - Generic types in both parameters and return values
 * - Type safety with independent type parameters
 * - Common cache/map implementation pattern
 *
 * **Expected behavior**:
 * ```kotlin
 * class FakeKeyValueCacheImpl<K, V> : KeyValueCache<K, V>() {
 *     private var putBehavior: (K, V) -> Unit = { key, value -> super.put(key, value) }
 *     private var getBehavior: (K) -> V? = { key -> super.get(key) }
 *     private var removeBehavior: (K) -> V? = { key -> super.remove(key) }
 *     private var containsKeyBehavior: (K) -> Boolean = { key -> super.containsKey(key) }
 *     private var sizeBehavior: () -> Int = { super.size }
 *
 *     override fun put(key: K, value: V) = putBehavior(key, value)
 *     override fun get(key: K): V? = getBehavior(key)
 *     // ...
 * }
 *
 * inline fun <K, V> fakeKeyValueCache(configure: FakeKeyValueCacheConfig<K, V>.() -> Unit = {}): KeyValueCache<K, V>
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * // String keys, User values
 * val userCache: KeyValueCache<String, User> = fakeKeyValueCache {
 *     get { key -> User(key, "Default User") }
 *     put { key, value -> println("Caching user: $key") }
 * }
 *
 * // Int keys, String values
 * val stringCache: KeyValueCache<Int, String> = fakeKeyValueCache {
 *     get { id -> "Item $id" }
 * }
 * ```
 */
@Fake
open class KeyValueCache<K, V> {
    /**
     * Stores a key-value pair in the cache.
     */
    open fun put(
        key: K,
        value: V,
    ) {
        // Simulate cache storage
    }

    /**
     * Retrieves the value associated with the key.
     */
    open fun get(key: K): V? = null

    /**
     * Removes and returns the value associated with the key.
     */
    open fun remove(key: K): V? = null

    /**
     * Checks if the cache contains the given key.
     */
    open fun containsKey(key: K): Boolean = false

    /**
     * Returns the number of entries in the cache.
     */
    open val size: Int
        get() = 0
}
