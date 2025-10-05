// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.stdlibTypes

import com.rsicarelli.fakt.Fake

/**
 * Scenario 30: Sequence mapper
 */
@Fake
fun interface SequenceMapper<T, R> {
    fun map(sequence: Sequence<T>): Sequence<R>
}
