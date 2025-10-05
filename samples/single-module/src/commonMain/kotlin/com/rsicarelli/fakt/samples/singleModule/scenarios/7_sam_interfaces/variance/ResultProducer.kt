// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 46: Covariant with Result
 */
@Fake
fun interface ResultProducer<out T> {
    fun produce(): Result<T>
}
