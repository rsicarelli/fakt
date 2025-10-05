// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.higherOrder

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
