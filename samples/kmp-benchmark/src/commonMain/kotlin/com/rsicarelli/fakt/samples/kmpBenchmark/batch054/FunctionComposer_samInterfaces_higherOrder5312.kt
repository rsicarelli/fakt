// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch054

import com.rsicarelli.fakt.Fake

@Fake
fun interface FunctionComposer_samInterfaces_higherOrder5312<T, U, R> {
    fun compose(
        fn1: (T) -> U,
        fn2: (U) -> R,
        input: T,
    ): R
}
