// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order

import com.rsicarelli.fakt.Fake

/**
 * Scenario 40: Predicate combiner
 */
@Fake
fun interface PredicateCombiner<T> {
    fun combine(p1: (T) -> Boolean, p2: (T) -> Boolean): (T) -> Boolean
}
