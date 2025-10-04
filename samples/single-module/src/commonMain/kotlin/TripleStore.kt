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
    fun get(key1: K1, key2: K2): V?
    fun put(key1: K1, key2: K2, value: V): V?
    fun contains(key1: K1, key2: K2): Boolean
    fun remove(key1: K1, key2: K2): V?
}
