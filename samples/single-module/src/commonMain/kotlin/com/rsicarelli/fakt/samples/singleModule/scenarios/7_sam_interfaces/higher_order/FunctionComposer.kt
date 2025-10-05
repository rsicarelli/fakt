// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order

import com.rsicarelli.fakt.Fake

/**
 * Scenario 39: Function composer
 */
@Fake
fun interface FunctionComposer<T, U, R> {
    fun compose(
        fn1: (T) -> U,
        fn2: (U) -> R,
        input: T,
    ): R
}
