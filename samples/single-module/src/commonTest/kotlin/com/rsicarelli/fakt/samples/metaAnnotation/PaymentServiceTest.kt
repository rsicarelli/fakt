// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.metaAnnotation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Demonstrates using a fake generated from a custom @TestDouble annotation.
 *
 * This test shows how the meta-annotation system enables companies to use their own
 * annotations for fake generation without any Gradle configuration.
 *
 * ## How It Works
 * 1. @TestDouble is marked with @GeneratesFake meta-annotation
 * 2. Compiler automatically detects @TestDouble on PaymentService interface
 * 3. Generates: FakePaymentServiceImpl, fakePaymentService(), FakePaymentServiceConfig
 * 4. Test uses generated fake just like with @Fake annotation
 */
class PaymentServiceTest {

    @Test
    fun `test payment processing with custom annotation`() {
        // Given - Create fake using generated factory function
        val paymentService = fakePaymentService {
            // Configure payment processing behavior
            processPayment { amount, currency ->
                when {
                    amount > 0 -> "TXN-${amount.toInt()}-$currency"
                    else -> null
                }
            }

            // Configure validation behavior
            validatePayment { cardNumber, cvv ->
                cardNumber.length == 16 && cvv.length == 3
            }
        }

        // When & Then - Use the fake in tests
        val txnId = paymentService.processPayment(100.0, "USD")
        assertEquals("TXN-100-USD", txnId)

        assertTrue(paymentService.validatePayment("1234567890123456", "123"))
        assertFalse(paymentService.validatePayment("invalid", "12"))
    }

    @Test
    fun `test suspend function refund with custom annotation`() = runTest {
        // Given
        val paymentService = fakePaymentService {
            refundTransaction { transactionId ->
                transactionId.startsWith("TXN-")
            }
        }

        // When & Then
        assertTrue(paymentService.refundTransaction("TXN-123"))
        assertFalse(paymentService.refundTransaction("INVALID"))
    }

    @Test
    fun `test default parameters with custom annotation`() {
        // Given
        val paymentService = fakePaymentService {
            processPayment { amount, currency ->
                "Payment: $amount $currency"
            }
        }

        // When - Using default currency parameter
        val result = paymentService.processPayment(50.0)

        // Then - Default "USD" is used
        assertEquals("Payment: 50.0 USD", result)
    }
}
