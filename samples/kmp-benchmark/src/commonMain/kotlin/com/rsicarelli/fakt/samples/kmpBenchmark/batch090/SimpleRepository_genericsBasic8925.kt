// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch090

import com.rsicarelli.fakt.Fake

@Fake
interface SimpleRepository_genericsBasic8925<T> {
    fun save(item: T): T

    fun findAll(): List<T>
}
