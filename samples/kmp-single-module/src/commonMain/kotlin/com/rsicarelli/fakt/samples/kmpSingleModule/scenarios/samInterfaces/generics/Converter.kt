// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.Fake

/**
 * Scenario 8: Two type parameters
 */
@Fake
fun interface Converter<T, R> {
    fun convert(input: T): R
}
