// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.abstractClass

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: Abstract class with suspend functions
 *
 * **Pattern**: AbstractClassWithSuspend
 * **Priority**: P1 (High - Common Use Case)
 *
 * **What it tests**:
 * - Suspend abstract methods (error default)
 * - Suspend open methods (super call default)
 * - Suspend modifier preserved in DSL
 *
 * **Expected behavior**:
 * ```kotlin
 * private var fetchDataBehavior: suspend (String) -> String = { _ -> error("Configure fetchData behavior") }
 * private var uploadBehavior: suspend (String) -> Boolean = { data -> super.upload(data) }
 * ```
 */
@Fake
abstract class AsyncDataFetcher {
    // Suspend abstract method
    abstract suspend fun fetchData(url: String): String

    // Suspend open method
    open suspend fun upload(data: String): Boolean = data.isNotEmpty()

    // Regular abstract method
    abstract fun validate(data: String): Boolean
}
