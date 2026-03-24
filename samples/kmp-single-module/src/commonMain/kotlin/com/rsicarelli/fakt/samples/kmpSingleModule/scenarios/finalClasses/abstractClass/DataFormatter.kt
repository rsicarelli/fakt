// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.abstractClass

import com.rsicarelli.fakt.Fake

/**
 * Regression: Abstract class with constructor parameters and generics (#70)
 *
 * **Pattern**: AbstractClassConstructorParamsGenerics
 *
 * **What it tests**:
 * - Abstract class with required constructor parameters (locale)
 * - Constructor parameters with default values (precision)
 * - Generic type parameter T on the class
 * - Generated fake must pass default values to super constructor
 *
 * **Previously failing**:
 * - `FakeDataFormatterImpl` called `DataFormatter<T>()` ignoring required params
 * - Config DSL methods used `= run { }` which failed in explicit API mode
 */
@Fake
abstract class DataFormatter<T>(
    private val locale: String,
    private val precision: Int = 2,
) {
    abstract fun format(): T

    abstract fun parse(): T
}
