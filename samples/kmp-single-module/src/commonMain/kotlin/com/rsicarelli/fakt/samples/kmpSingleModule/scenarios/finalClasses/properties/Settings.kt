// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.properties

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: ClassWithMutableProperties
 *
 * **Pattern**: Open class with mutable properties (var with getter + setter)
 * **Priority**: P1 (High - Common State Management Pattern)
 *
 * **What it tests**:
 * - Open var properties with both getter and setter
 * - Setter behavior can be configured separately
 * - Getter and setter work together correctly
 * - State management in fake objects
 *
 * **Expected behavior**:
 * ```kotlin
 * // Mutable property getter
 * private var themeBehavior: () -> String = { super.theme }
 * // Mutable property setter
 * private var setThemeBehavior: (String) -> Unit = { value -> super.theme = value }
 *
 * override var theme: String
 *     get() = themeBehavior()
 *     set(value) { setThemeBehavior(value) }
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * open class Settings {
 *     open var theme: String = "light"
 *     open var fontSize: Int = 14
 *     open var isAutoSaveEnabled: Boolean = true
 * }
 * ```
 */
@Fake
open class Settings {
    // Mutable properties with default values
    open var theme: String = "light"

    open var fontSize: Int = 14

    open var isAutoSaveEnabled: Boolean = true

    // Method that uses mutable properties
    open fun resetToDefaults() {
        theme = "light"
        fontSize = 14
        isAutoSaveEnabled = true
    }
}
