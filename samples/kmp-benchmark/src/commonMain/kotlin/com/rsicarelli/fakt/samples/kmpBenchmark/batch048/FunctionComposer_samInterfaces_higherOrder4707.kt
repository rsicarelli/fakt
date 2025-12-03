// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch048

import com.rsicarelli.fakt.Fake

@Fake
fun interface FunctionComposer_samInterfaces_higherOrder4707<T, U, R> {
    fun compose(
        fn1: (T) -> U,
        fn2: (U) -> R,
        input: T,
    ): R
}
