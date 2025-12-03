// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch001

import com.rsicarelli.fakt.Fake

@Fake
interface DataProcessor_genericsMethodLevel14 {
    fun <T> process(item: T): T

    fun <R> transform(input: String): R

    fun getData(): String
}
