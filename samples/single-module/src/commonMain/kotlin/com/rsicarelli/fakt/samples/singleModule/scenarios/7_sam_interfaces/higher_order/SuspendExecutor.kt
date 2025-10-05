// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order

import com.rsicarelli.fakt.Fake

/**
 * Scenario 38: Suspend function executor
 */
@Fake
fun interface SuspendExecutor<T, R> {
    suspend fun execute(
        fn: suspend (T) -> R,
        input: T,
    ): R
}
