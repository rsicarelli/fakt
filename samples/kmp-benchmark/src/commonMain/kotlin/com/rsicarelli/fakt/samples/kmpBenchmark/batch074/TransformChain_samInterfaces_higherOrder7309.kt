// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch074

import com.rsicarelli.fakt.Fake

@Fake
fun interface TransformChain_samInterfaces_higherOrder7309<T, U, R> {
    fun chain(
        input: T,
        first: (T) -> U,
        second: (U) -> R,
    ): R
}
