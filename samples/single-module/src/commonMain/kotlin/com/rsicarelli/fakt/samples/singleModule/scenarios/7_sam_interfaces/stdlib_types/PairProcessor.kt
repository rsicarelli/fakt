// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types

import com.rsicarelli.fakt.Fake

/**
 * Scenario 31: Pair processor
 */
@Fake
fun interface PairProcessor<T> {
    fun process(pair: Pair<T, T>): T
}
