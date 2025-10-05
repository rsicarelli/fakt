// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * P0 Scenario: Open class with mix of open and final methods
 *
 * **Pattern**: ClassWithFinalMethods
 * **Priority**: P0 (Critical - Edge Case)
 *
 * **What it tests**:
 * - Final methods are ignored (not included in fake)
 * - Open methods can be configured
 * - Final methods use original implementation directly
 *
 * **Expected behavior**:
 * ```kotlin
 * // Only open methods get behavior properties
 * private var loadConfigBehavior: (String) -> String = { key -> super.loadConfig(key) }
 * private var saveConfigBehavior: (String, String) -> Unit = { key, value -> super.saveConfig(key, value) }
 *
 * // validateKey and getVersion are final - NOT included in fake
 * ```
 */
@Fake
open class ConfigManager {
    // Open methods - can be overridden
    open fun loadConfig(key: String): String = "default-$key"

    open fun saveConfig(
        key: String,
        value: String,
    ) {
        println("Saving $key = $value")
    }

    // Final methods - cannot be overridden (not included in fake)
    fun validateKey(key: String): Boolean = key.isNotEmpty() && key.length <= 50

    fun getVersion(): String = "1.0.0"
}
