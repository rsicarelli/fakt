// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order

import com.rsicarelli.fakt.Fake

/**
 * Scenario 37: Function executor
 */
@Fake
fun interface FunctionExecutor<T, R> {
    fun execute(
        fn: (T) -> R,
        input: T,
    ): R
}
