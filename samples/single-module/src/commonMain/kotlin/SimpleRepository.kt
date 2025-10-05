// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * P0.0: Single type parameter test interface.
 *
 * Tests basic generic support for interfaces with a single type parameter (T).
 * This is the baseline for generic fake generation, validating that the compiler
 * can generate: class FakeSimpleRepository<T> : SimpleRepository<T>
 */
@Fake
interface SimpleRepository<T> {
    fun save(item: T): T

    fun findAll(): List<T>
}
