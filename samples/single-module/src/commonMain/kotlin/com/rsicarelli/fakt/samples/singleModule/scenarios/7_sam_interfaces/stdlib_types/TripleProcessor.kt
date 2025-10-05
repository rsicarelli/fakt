// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.stdlibTypes

import com.rsicarelli.fakt.Fake

/**
 * Scenario 36c: Triple processor - transforms Triple<A,B,C> to Triple<D,E,F>
 */
@Fake
fun interface TripleProcessor<A, B, C, D, E, F> {
    fun process(triple: Triple<A, B, C>): Triple<D, E, F>
}
