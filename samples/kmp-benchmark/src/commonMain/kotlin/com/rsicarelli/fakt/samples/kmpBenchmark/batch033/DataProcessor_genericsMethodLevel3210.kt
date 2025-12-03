// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch033

import com.rsicarelli.fakt.Fake

@Fake
interface DataProcessor_genericsMethodLevel3210 {
    fun <T> process(item: T): T

    fun <R> transform(input: String): R

    fun getData(): String
}
