// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.metaAnnotations

import com.rsicarelli.fakt.samples.singlemodule.models.CustomAnnotation

/**
 * Example payment service interface using custom @TestDouble annotation.
 *
 * This demonstrates how companies can use their own annotations instead of @Fake
 * for fake generation. The @TestDouble annotation is marked with @GeneratesFake,
 * so the compiler automatically detects it without any Gradle configuration.
 */
@CustomAnnotation
interface PaymentService {
    /**
     * Process a payment transaction.
     *
     * @param amount The payment amount
     * @param currency The payment currency (default: USD)
     * @return Transaction ID if successful, null if failed
     */
    fun processPayment(
        amount: Double,
        currency: String,
    ): String?

    /**
     * Validate payment details.
     *
     * @param cardNumber Credit card number
     * @param cvv Security code
     * @return true if valid, false otherwise
     */
    fun validatePayment(
        cardNumber: String,
        cvv: String,
    ): Boolean

    /**
     * Refund a transaction.
     *
     * @param transactionId The transaction to refund
     * @return true if refund successful, false otherwise
     */
    suspend fun refundTransaction(transactionId: String): Boolean
}
