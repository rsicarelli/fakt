// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types

import com.rsicarelli.fakt.Fake

/**
 * Scenario 35: Result with function parameter (using class generics instead of method generics)
 */
@Fake
fun interface ResultFunctionHandler<T, R> {
    fun handle(
        result: Result<T>,
        mapper: (T) -> R,
    ): Result<R>
}
