// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch093

import com.rsicarelli.fakt.Fake

@Fake
fun interface SuspendExecutor_samInterfaces_higherOrder9258<T, R> {
    suspend fun execute(
        fn: suspend (T) -> R,
        input: T,
    ): R
}
