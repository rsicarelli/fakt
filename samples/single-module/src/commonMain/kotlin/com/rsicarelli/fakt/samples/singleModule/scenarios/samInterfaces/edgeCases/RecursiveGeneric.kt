// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * Scenario 53a: Recursive generic (alias for RecursiveComparator)
 */
@Fake
fun interface RecursiveGeneric<T : Comparable<T>> {
    fun process(item: T): Int
}
