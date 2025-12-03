// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch023

import com.rsicarelli.fakt.Fake

@Fake
interface MixedProcessor_genericsMethodLevel2291<T> {
    fun process(item: T): T

    fun <R> transform(item: T): R

    fun reset()
}
