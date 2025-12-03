// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch087

import com.rsicarelli.fakt.Fake

@Fake
fun interface SequenceFilter_samInterfaces_stdlibTypes8699<T> {
    fun filter(
        sequence: Sequence<T>,
        predicate: (T) -> Boolean,
    ): Sequence<T>
}
