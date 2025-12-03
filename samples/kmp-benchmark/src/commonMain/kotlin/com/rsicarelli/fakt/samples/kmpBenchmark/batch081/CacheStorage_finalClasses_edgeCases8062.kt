// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch081

import com.rsicarelli.fakt.Fake

@Fake
open class CacheStorage_finalClasses_edgeCases8062 {
    open fun get(key: String): String? = null

    open fun put(
        key: String,
        value: String?,
    ) {
        println("Put: $key = $value")
    }

    open fun find(prefix: String?): List<String> = if (prefix == null) emptyList() else listOf(prefix)

    open fun remove(key: String): String? = null
}
