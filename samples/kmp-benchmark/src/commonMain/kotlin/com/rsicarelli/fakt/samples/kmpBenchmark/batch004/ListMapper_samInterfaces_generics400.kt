// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch004

import com.rsicarelli.fakt.Fake

@Fake
fun interface ListMapper_samInterfaces_generics400<T, R> {
    fun map(items: List<T>): List<R>
}
