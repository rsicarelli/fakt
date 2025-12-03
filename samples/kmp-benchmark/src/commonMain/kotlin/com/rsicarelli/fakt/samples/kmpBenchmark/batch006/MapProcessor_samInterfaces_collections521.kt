// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch006

import com.rsicarelli.fakt.Fake

@Fake
fun interface MapProcessor_samInterfaces_collections521<K, V, R> {
    fun process(map: Map<K, V>): Map<K, R>
}
