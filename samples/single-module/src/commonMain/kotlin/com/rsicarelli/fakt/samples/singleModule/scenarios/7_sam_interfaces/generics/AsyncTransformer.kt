// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.Fake

/**
 * Scenario 14: Generic SAM with suspend
 */
@Fake
fun interface AsyncTransformer<T> {
    suspend fun transform(input: T): T
}
