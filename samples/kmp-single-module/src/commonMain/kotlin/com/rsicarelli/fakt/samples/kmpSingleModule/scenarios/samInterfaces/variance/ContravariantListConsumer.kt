// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 48e: Contravariant list consumer
 */
@Fake
fun interface ContravariantListConsumer<in T> {
    fun consume(list: List<T>)
}
