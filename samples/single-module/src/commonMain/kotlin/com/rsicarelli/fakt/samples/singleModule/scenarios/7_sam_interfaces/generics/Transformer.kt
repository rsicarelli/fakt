// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.Fake

/**
 * Scenario 7: Single type parameter
 */
@Fake
fun interface Transformer<T> {
    fun transform(input: T): T
}
