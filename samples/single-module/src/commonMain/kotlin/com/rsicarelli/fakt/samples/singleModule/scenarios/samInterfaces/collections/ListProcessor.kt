// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 21: List processor
 */
@Fake
fun interface ListProcessor<T> {
    fun process(items: List<T>): List<T>
}
