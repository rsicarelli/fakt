// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.operators.collections

import com.rsicarelli.fakt.Fake

/**
 * Test interface for collection-like operator functions.
 *
 * Covers: get ([]), set ([]=), contains (in), iterator (for loop)
 *
 * Expected behavior:
 * - Generated fake should support collection operator syntax: container[key], key in container, etc.
 * - Default behavior can be configured via DSL
 * - Call tracking should work normally
 */
@Fake
interface ContainerOperations<K, V> {
    /**
     * Index access operator (getter).
     * Usage: container[key]
     */
    operator fun get(key: K): V?

    /**
     * Index assignment operator (setter).
     * Usage: container[key] = value
     */
    operator fun set(key: K, value: V)

    /**
     * Contains operator.
     * Usage: key in container
     */
    operator fun contains(key: K): Boolean

    /**
     * Iterator operator for for-each loops.
     * Usage: for ((k, v) in container) { ... }
     */
    operator fun iterator(): Iterator<Pair<K, V>>
}

/**
 * Test interface for range operators.
 */
@Fake
interface RangeOperations {
    /**
     * Range-to operator.
     * Usage: 1..10
     */
    operator fun Int.rangeTo(other: Int): IntRange

    /**
     * Range-until operator.
     * Usage: 1..<10
     */
    operator fun Int.rangeUntil(other: Int): IntRange
}

/**
 * Test interface for comparison operators.
 */
@Fake
interface ComparisonOperations<T : Comparable<T>> {
    /**
     * Comparison operator.
     * Enables: <, >, <=, >= operators
     * Usage: a > b, a <= b, etc.
     */
    operator fun compareTo(other: T): Int
}
