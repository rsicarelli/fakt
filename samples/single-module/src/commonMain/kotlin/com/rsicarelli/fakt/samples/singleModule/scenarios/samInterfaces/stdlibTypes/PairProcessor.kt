// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.stdlibTypes

import com.rsicarelli.fakt.Fake

/**
 * Scenario 31: Pair processor
 */
@Fake
fun interface PairProcessor<T> {
    fun process(pair: Pair<T, T>): T
}
