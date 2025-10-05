// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * Scenario 51: SAM with recursive generic
 */
@Fake
fun interface RecursiveComparator<T : Comparable<T>> {
    fun compare(
        a: T,
        b: T,
    ): Int
}
