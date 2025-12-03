// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch036

import com.rsicarelli.fakt.Fake

@Fake
fun interface MapWithFunction_samInterfaces_collections3529<T, R> {
    fun transform(
        items: List<T>,
        mapper: (T) -> R,
    ): List<R>
}
