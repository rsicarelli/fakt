// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 48e: Contravariant list consumer
 */
@Fake
fun interface ContravariantListConsumer<in T> {
    fun consume(list: List<T>)
}
