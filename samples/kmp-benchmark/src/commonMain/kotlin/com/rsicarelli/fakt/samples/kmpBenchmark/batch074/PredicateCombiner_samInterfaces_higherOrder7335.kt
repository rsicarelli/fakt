// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch074

import com.rsicarelli.fakt.Fake

@Fake
fun interface PredicateCombiner_samInterfaces_higherOrder7335<T> {
    fun combine(
        p1: (T) -> Boolean,
        p2: (T) -> Boolean,
    ): (T) -> Boolean
}
