// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch100

import com.rsicarelli.fakt.Fake

@Fake
interface DataCache_genericsBasic9945<T> {
    fun storeBatch(items: List<T>)

    fun getAllBatches(): List<List<T>>

    fun storeGroups(groups: Map<String, List<T>>)

    fun getGroup(name: String): List<T>?

    fun storeNested(data: List<List<T>>)

    fun getMatrix(): List<List<T>>
}
