// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 48a: Covariant producer (explicit name for tests)
 */
@Fake
fun interface CovariantProducer<out T> {
    fun produce(): T
}
