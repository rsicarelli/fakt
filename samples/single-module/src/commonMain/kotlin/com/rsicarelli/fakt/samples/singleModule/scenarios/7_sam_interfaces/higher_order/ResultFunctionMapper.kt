// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order

import com.rsicarelli.fakt.Fake

/**
 * Scenario 42: Result mapper with function
 */
@Fake
fun interface ResultFunctionMapper<T, R> {
    fun mapResult(fn: (T) -> R, input: T): Result<R>
}
