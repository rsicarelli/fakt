// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.higherOrder

import com.rsicarelli.fakt.Fake

/**
 * Scenario 42a: Predicate filter
 */
@Fake
fun interface PredicateFilter<T> {
    fun filter(
        items: List<T>,
        predicate: (T) -> Boolean,
    ): List<T>
}
