// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types

import com.rsicarelli.fakt.Fake

/**
 * Scenario 36b: Pair mapper - transforms Pair<A,B> to Pair<C,D>
 */
@Fake
fun interface PairMapper<A, B, C, D> {
    fun map(pair: Pair<A, B>): Pair<C, D>
}
