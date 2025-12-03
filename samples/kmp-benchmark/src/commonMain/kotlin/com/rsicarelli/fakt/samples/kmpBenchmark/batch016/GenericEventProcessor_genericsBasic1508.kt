// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch016

import com.rsicarelli.fakt.Fake

@Fake
interface GenericEventProcessor_genericsBasic1508<T> {
    fun process(
        item: T,
        processor: (T) -> String,
    ): String

    fun <R> transform(
        items: List<T>,
        transformer: (T) -> R,
    ): List<R>
}
