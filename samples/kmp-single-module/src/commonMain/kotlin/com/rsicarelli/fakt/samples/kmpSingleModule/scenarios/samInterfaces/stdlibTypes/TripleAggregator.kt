// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.stdlibTypes

import com.rsicarelli.fakt.Fake

/**
 * Scenario 32: Triple aggregator
 */
@Fake
fun interface TripleAggregator<T> {
    fun aggregate(triple: Triple<T, T, T>): T
}
