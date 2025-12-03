// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch020

import com.rsicarelli.fakt.Fake

@Fake
fun interface ComparableProcessor_samInterfaces_generics1935<T : Comparable<T>> {
    fun process(item: T): T
}
