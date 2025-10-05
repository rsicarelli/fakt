// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.Fake

/**
 * Scenario 9: Generic with constraint
 */
@Fake
fun interface ComparableProcessor<T : Comparable<T>> {
    fun process(item: T): T
}
