// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch047

import com.rsicarelli.fakt.Fake

@Fake
fun interface PredicateFilter_samInterfaces_higherOrder4617<T> {
    fun filter(
        items: List<T>,
        predicate: (T) -> Boolean,
    ): List<T>
}
