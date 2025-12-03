// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch061

import com.rsicarelli.fakt.Fake

@Fake
fun interface NestedGenericMapper_samInterfaces_edgeCases6023<T, R> {
    fun map(nested: List<List<T>>): List<List<R>>
}
