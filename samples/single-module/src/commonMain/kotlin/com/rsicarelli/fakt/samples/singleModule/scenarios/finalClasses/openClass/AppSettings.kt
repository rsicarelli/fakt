// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.openClass

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: Open class with open properties
 *
 * **Pattern**: OpenClassWithProperties
 * **Priority**: P1 (High - Common Use Case)
 *
 * **What it tests**:
 * - Open properties (val/var) can be overridden
 * - Properties use super call defaults
 * - Mix of properties and methods
 *
 * **Expected behavior**:
 * ```kotlin
 * private var themeBehavior: () -> String = { super.theme }
 * private var isDarkModeBehavior: () -> Boolean = { super.isDarkMode }
 * private var maxRetriesBehavior: () -> Int = { super.maxRetries }
 * private var getSettingBehavior: (String) -> String? = { key -> super.getSetting(key) }
 * ```
 */
@Fake
open class AppSettings {
    // Open properties
    open val theme: String
        get() = "light"

    open val isDarkMode: Boolean
        get() = false

    open val maxRetries: Int
        get() = 3

    // Open method
    open fun getSetting(key: String): String? = null
}
