// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch043

import com.rsicarelli.fakt.Fake

@Fake
interface GenericRepository_genericsMethodLevel4227<T> {
    val items: List<T>

    fun findAll(): List<T>

    fun findById(id: String): T?

    fun save(item: T): T

    fun saveAll(items: List<T>): List<T>

    fun <R> map(transformer: (T) -> R): List<R>
}
