// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.propertiesAndMethods

import com.rsicarelli.fakt.Fake

/**
 * Basic interface with properties and methods (no generics).
 *
 * Tests fundamental fake generation for interfaces combining:
 * - Read-only property (val stringValue)
 * - Getter method (getValue)
 * - Setter method (setValue)
 * Validates baseline code generation without generic complexity.
 */
@Fake
interface PropertyAndMethodInterface {
    val stringValue: String

    fun getValue(): String

    fun setValue(value: String)
}
