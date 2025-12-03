// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch068

import com.rsicarelli.fakt.Fake

@Fake
fun interface FunctionExecutor_samInterfaces_higherOrder6740<T, R> {
    fun execute(
        fn: (T) -> R,
        input: T,
    ): R
}
