// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.Fake

/**
 * Scenario 10: Generic with multiple constraints
 */
@Fake
fun interface MultiConstraintHandler<T> where T : CharSequence, T : Comparable<T> {
    fun handle(item: T): Int
}
