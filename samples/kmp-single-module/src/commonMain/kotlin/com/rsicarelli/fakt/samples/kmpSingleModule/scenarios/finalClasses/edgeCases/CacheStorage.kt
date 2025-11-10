// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: Open class with nullable types
 *
 * **Pattern**: ClassWithNullableTypes
 * **Priority**: P1 (High - Common Use Case)
 *
 * **What it tests**:
 * - Nullable parameters (String?, Int?, etc.)
 * - Nullable return types
 * - Null handling in defaults
 *
 * **Expected behavior**:
 * ```kotlin
 * private var getBehavior: (String) -> String? = { key -> super.get(key) }
 * private var putBehavior: (String, String?) -> Unit = { key, value -> super.put(key, value) }
 * private var findBehavior: (String?) -> List<String> = { prefix -> super.find(prefix) }
 * ```
 */
@Fake
open class CacheStorage {
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
