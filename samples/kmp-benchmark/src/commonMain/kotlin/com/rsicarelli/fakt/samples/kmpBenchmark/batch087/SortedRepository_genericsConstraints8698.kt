// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch087

import com.rsicarelli.fakt.Fake

@Fake
interface SortedRepository_genericsConstraints8698<T : Comparable<T>> {
    fun insert(item: T)

    fun findMin(): T?

    fun findMax(): T?

    fun getAll(): List<T>

    fun sort(): List<T>
}
