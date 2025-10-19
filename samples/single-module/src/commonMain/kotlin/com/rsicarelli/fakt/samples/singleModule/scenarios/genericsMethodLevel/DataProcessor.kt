// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.genericsMethodLevel

import com.rsicarelli.fakt.Fake

/**
 * Phase 3 test interface: Method-level generics only (no class-level generics).
 *
 * Tests that method-level type parameters are preserved:
 * - fun <T> process(item: T): T
 * - fun <R> transform(input: String): R
 */
@Fake
interface DataProcessor {
    fun <T> process(item: T): T

    fun <R> transform(input: String): R

    fun getData(): String
}
