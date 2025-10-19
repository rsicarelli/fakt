// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsMethodLevel

import com.rsicarelli.fakt.Fake

/**
 * P1.2: Generic repository with method-level type parameters.
 *
 * Tests combination of class-level generic (T) with method-level type parameters (<R>).
 * Common in repository patterns where you have a base entity type (T) but need to
 * support transformations to other types (R) via map operations.
 */
@Fake
interface GenericRepository<T> {
    val items: List<T>

    fun findAll(): List<T>

    fun findById(id: String): T?

    fun save(item: T): T

    fun saveAll(items: List<T>): List<T>

    fun <R> map(transformer: (T) -> R): List<R>
}
