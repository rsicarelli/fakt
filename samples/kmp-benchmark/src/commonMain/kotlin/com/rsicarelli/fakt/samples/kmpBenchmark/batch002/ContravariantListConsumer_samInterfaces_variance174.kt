// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch002

import com.rsicarelli.fakt.Fake

@Fake
fun interface ContravariantListConsumer_samInterfaces_variance174<in T> {
    fun consume(list: List<T>)
}
