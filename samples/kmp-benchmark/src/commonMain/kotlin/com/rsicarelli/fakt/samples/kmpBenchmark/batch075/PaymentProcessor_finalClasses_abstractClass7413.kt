// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch075

import com.rsicarelli.fakt.Fake

@Fake
abstract class PaymentProcessor_finalClasses_abstractClass7413 {
    abstract fun process(amount: Double): Boolean

    abstract fun validate(cardNumber: String): Boolean

    abstract fun refund(transactionId: String): Double

    abstract fun getFee(amount: Double): Double
}
