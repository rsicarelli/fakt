// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.mixed

import com.rsicarelli.fakt.Fake

/**
 * P0 Scenario: Abstract class with both abstract and open methods
 *
 * **Pattern**: MixedAbstractAndOpen
 * **Priority**: P0 (Critical)
 *
 * **What it tests**:
 * - Abstract methods (must configure - error default)
 * - Open methods (optional - super call default)
 * - Mixing both patterns in single class
 *
 * **Expected behavior**:
 * ```kotlin
 * // Abstract - error default
 * private var fetchDataBehavior: () -> String = { error("Configure fetchData behavior") }
 * private var validateBehavior: (String) -> Boolean = { _ -> error("Configure validate behavior") }
 *
 * // Open - super call default
 * private var transformBehavior: (String) -> String = { data -> super.transform(data) }
 * private var logBehavior: (String) -> Unit = { message -> super.log(message) }
 * ```
 */
@Fake
abstract class DataService {
    // Abstract methods - must be overridden
    abstract fun fetchData(): String

    abstract fun validate(data: String): Boolean

    // Open methods - can be overridden
    open fun transform(data: String): String = data.uppercase()

    open fun log(message: String) {
        println("[LOG] $message")
    }
}
