// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * P0.3: Three type parameters test interface.
 *
 * Tests generic support for interfaces with three type parameters (K1, K2, V).
 * This validates the compiler can handle complex multi-parameter scenarios.
 */
@Fake
interface TripleStore<K1, K2, V> {
    /**
     * Retrieve a value using two keys.
     */
    fun get(
        key1: K1,
        key2: K2,
    ): V?

    /**
     * Store a value with two keys.
     */
    fun put(
        key1: K1,
        key2: K2,
        value: V,
    ): V?

    /**
     * Check if the key pair exists.
     */
    fun contains(
        key1: K1,
        key2: K2,
    ): Boolean

    /**
     * Remove a value by key pair.
     */
    fun remove(
        key1: K1,
        key2: K2,
    ): V?
}
