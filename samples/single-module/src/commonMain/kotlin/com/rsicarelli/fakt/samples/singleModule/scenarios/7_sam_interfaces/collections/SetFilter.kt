// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 23: Set filter
 */
@Fake
fun interface SetFilter<T> {
    fun filter(items: Set<T>): Set<T>
}
