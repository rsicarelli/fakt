// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order

import com.rsicarelli.fakt.Fake

/**
 * Scenario 42b: Transform chain - compose two transforms
 */
@Fake
fun interface TransformChain<T, U, R> {
    fun chain(
        input: T,
        first: (T) -> U,
        second: (U) -> R,
    ): R
}
