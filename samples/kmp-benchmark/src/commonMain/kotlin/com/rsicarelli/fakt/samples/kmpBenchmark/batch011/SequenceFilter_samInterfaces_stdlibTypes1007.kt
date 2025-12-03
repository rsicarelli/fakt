// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch011

import com.rsicarelli.fakt.Fake

@Fake
fun interface SequenceFilter_samInterfaces_stdlibTypes1007<T> {
    fun filter(
        sequence: Sequence<T>,
        predicate: (T) -> Boolean,
    ): Sequence<T>
}
