// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 47: Contravariant with List
 */
@Fake
fun interface ListConsumer<in T> {
    fun consume(items: List<T>)
}
