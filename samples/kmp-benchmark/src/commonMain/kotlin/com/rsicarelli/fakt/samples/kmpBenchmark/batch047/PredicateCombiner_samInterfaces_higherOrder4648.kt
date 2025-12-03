// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch047

import com.rsicarelli.fakt.Fake

@Fake
fun interface PredicateCombiner_samInterfaces_higherOrder4648<T> {
    fun combine(
        p1: (T) -> Boolean,
        p2: (T) -> Boolean,
    ): (T) -> Boolean
}
