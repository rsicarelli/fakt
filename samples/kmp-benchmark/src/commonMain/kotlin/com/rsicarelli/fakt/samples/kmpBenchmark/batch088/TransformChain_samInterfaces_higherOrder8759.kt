// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch088

import com.rsicarelli.fakt.Fake

@Fake
fun interface TransformChain_samInterfaces_higherOrder8759<T, U, R> {
    fun chain(
        input: T,
        first: (T) -> U,
        second: (U) -> R,
    ): R
}
