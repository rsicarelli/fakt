// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.Fake

/**
 * Scenario 11: Generic SAM with nullable type
 */
@Fake
fun interface NullableTransformer<T> {
    fun transform(input: T?): T?
}
