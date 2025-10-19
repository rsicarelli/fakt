// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 48d: Covariant list producer
 */
@Fake
fun interface CovariantListProducer<out T> {
    fun produce(): List<T>
}
