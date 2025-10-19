// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.generics

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: GenericClassWithConstraint
 *
 * **Pattern**: Open class with generic type parameter constrained to Comparable
 * **Priority**: P1 (High - Common Sorted Collection Pattern)
 *
 * **What it tests**:
 * - Generic type parameter with upper bound constraint
 * - T : Comparable<T> pattern (self-comparable)
 * - Type-safe operations requiring ordered types
 * - Constraint preservation in generated code
 *
 * **Expected behavior**:
 * ```kotlin
 * class FakeSortedRepositoryImpl<T : Comparable<T>> : SortedRepository<T>() {
 *     private var addBehavior: (T) -> Unit = { item -> super.add(item) }
 *     private var findMinBehavior: () -> T? = { super.findMin() }
 *     private var findMaxBehavior: () -> T? = { super.findMax() }
 *     private var getSortedBehavior: () -> List<T> = { super.getSorted() }
 *     // ...
 * }
 *
 * inline fun <T : Comparable<T>> fakeSortedRepository(
 *     configure: FakeSortedRepositoryConfig<T>.() -> Unit = {}
 * ): SortedRepository<T>
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * // Works with Int (implements Comparable<Int>)
 * val numberRepo: SortedRepository<Int> = fakeSortedRepository {
 *     findMin { -10 }
 *     findMax { 100 }
 * }
 *
 * // Works with String (implements Comparable<String>)
 * val stringRepo: SortedRepository<String> = fakeSortedRepository {
 *     getSorted { listOf("alpha", "beta", "gamma") }
 * }
 *
 * // Won't compile with non-comparable types:
 * // val userRepo: SortedRepository<User> = ... // ERROR if User not Comparable
 * ```
 */
@Fake
open class SortedRepository<T : Comparable<T>> {
    /**
     * Adds an item to the repository (maintains sorted order).
     */
    open fun add(item: T) {
        // Simulate adding to sorted collection
    }

    /**
     * Finds the minimum item in the repository.
     */
    open fun findMin(): T? = null

    /**
     * Finds the maximum item in the repository.
     */
    open fun findMax(): T? = null

    /**
     * Returns all items in sorted order.
     */
    open fun getSorted(): List<T> = emptyList()
}
