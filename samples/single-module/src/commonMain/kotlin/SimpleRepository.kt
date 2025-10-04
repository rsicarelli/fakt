// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * Simple class-level generic interface for Phase 2 validation.
 * Tests that ImplementationGenerator generates: class FakeSimpleRepository<T> : SimpleRepository<T>
 */
@Fake
interface SimpleRepository<T> {
    fun save(item: T): T

    fun findAll(): List<T>
}
