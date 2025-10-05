// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.abstractClass

import com.rsicarelli.fakt.Fake

/**
 * P0 Scenario: Abstract class with multiple abstract methods
 *
 * **Pattern**: AbstractClassMultipleAbstract
 * **Priority**: P0 (Critical)
 *
 * **What it tests**:
 * - Multiple abstract methods requiring configuration
 * - All methods must have error defaults
 * - Independent configuration per method
 *
 * **Expected behavior**:
 * ```kotlin
 * private var processBehavior: (Double) -> Boolean = { _ -> error("Configure process behavior") }
 * private var validateBehavior: (String) -> Boolean = { _ -> error("Configure validate behavior") }
 * private var refundBehavior: (String) -> Double = { _ -> error("Configure refund behavior") }
 * private var getFeeBehavior: (Double) -> Double = { _ -> error("Configure getFee behavior") }
 * ```
 */
@Fake
abstract class PaymentProcessor {
    abstract fun process(amount: Double): Boolean

    abstract fun validate(cardNumber: String): Boolean

    abstract fun refund(transactionId: String): Double

    abstract fun getFee(amount: Double): Double
}
