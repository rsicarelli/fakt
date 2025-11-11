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
 * Custom types for range operator testing.
 * Using custom types avoids shadowing built-in Int.rangeTo() and Int.rangeUntil().
 */
data class CustomNumber(val value: Int)
data class CustomRange(val start: Int, val endInclusive: Int)

/**
 * Test interface for range operators with extension functions.
 * Tests Fakt's ability to generate fakes for operator extension functions.
 */
@Fake
interface RangeOperations {
    /**
     * Range-to operator for custom numbers.
     * Usage: customNum1..customNum2
     */
    operator fun CustomNumber.rangeTo(other: CustomNumber): CustomRange

    /**
     * Range-until operator for custom numbers.
     * Usage: customNum1..<customNum2
     */
    operator fun CustomNumber.rangeUntil(other: CustomNumber): CustomRange
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
