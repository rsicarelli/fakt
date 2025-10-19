// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.Fake

/**
 * Scenario 12: Generic SAM with List
 */
@Fake
fun interface ListMapper<T, R> {
    fun map(items: List<T>): List<R>
}
