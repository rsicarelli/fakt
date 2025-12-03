// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch079

import com.rsicarelli.fakt.Fake

@Fake
fun interface CollectionFilter_samInterfaces_collections7879<T> {
    fun filter(
        items: Collection<T>,
        predicate: (T) -> Boolean,
    ): Collection<T>
}
