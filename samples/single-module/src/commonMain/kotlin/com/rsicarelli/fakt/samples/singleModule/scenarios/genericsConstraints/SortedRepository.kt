// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.genericsConstraints

import com.rsicarelli.fakt.Fake

/**
 * P1.1: Type Constraint Support Test Interface
 *
 * Tests generic type parameter with single constraint.
 * Expected: Generated code should preserve `T : Comparable<T>` constraint.
 */
@Fake
interface SortedRepository<T : Comparable<T>> {
    fun insert(item: T)

    fun findMin(): T?

    fun findMax(): T?

    fun getAll(): List<T>

    fun sort(): List<T>
}
