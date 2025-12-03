// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch001

import com.rsicarelli.fakt.Fake

@Fake
fun interface ListConsumer_samInterfaces_variance36<in T> {
    fun consume(items: List<T>)
}
