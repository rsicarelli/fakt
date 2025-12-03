// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch029

import com.rsicarelli.fakt.Fake

@Fake
fun interface NestedGenericMapper_samInterfaces_edgeCases2900<T, R> {
    fun map(nested: List<List<T>>): List<List<R>>
}
