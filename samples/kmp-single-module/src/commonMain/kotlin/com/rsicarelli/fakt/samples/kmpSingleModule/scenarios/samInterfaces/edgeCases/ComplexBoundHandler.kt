// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * Scenario 53c: Complex bound handler with multiple constraints
 */
@Fake
fun interface ComplexBoundHandler<T> where T : CharSequence, T : Comparable<T> {
    fun handle(item: T): Int
}
