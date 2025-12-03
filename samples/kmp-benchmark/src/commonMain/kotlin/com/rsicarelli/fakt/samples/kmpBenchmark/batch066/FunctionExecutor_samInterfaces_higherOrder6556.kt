// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch066

import com.rsicarelli.fakt.Fake

@Fake
fun interface FunctionExecutor_samInterfaces_higherOrder6556<T, R> {
    fun execute(
        fn: (T) -> R,
        input: T,
    ): R
}
