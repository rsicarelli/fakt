// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.abstractClass

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for P0 Scenario: AbstractClassMultipleAbstract
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
class PaymentProcessorTest {
    @Test
    fun `GIVEN abstract class WHEN no methods configured THEN all should throw error`() {
        // Given
        val processor = fakePaymentProcessor {}

        // When/Then - all abstract methods should error
        assertFailsWith<IllegalStateException> { processor.process(100.0) }
        assertFailsWith<IllegalStateException> { processor.validate("1234") }
        assertFailsWith<IllegalStateException> { processor.refund("tx-1") }
        assertFailsWith<IllegalStateException> { processor.getFee(50.0) }
    }

    @Test
    fun `GIVEN abstract class WHEN all methods configured THEN should use configured behaviors`() {
        // Given
        val processor =
            fakePaymentProcessor {
                process { amount -> amount > 0 }
                validate { card -> card.length == 16 }
                refund { txId -> 100.0 }
                getFee { amount -> amount * 0.03 }
            }

        // When
        val processSuccess = processor.process(150.0)
        val processFail = processor.process(-10.0)
        val validCard = processor.validate("1234567890123456")
        val invalidCard = processor.validate("123")
        val refundAmount = processor.refund("tx-1")
        val fee = processor.getFee(100.0)

        // Then
        assertTrue(processSuccess, "positive amount should process")
        assertFalse(processFail, "negative amount should not process")
        assertTrue(validCard, "16-digit card should be valid")
        assertFalse(invalidCard, "short card should be invalid")
        assertEquals(100.0, refundAmount)
        assertEquals(3.0, fee, "fee should be 3% of 100")
    }

    @Test
    fun `GIVEN abstract class WHEN partially configured THEN configured work and others error`() {
        // Given - only configure validate and process
        val processor =
            fakePaymentProcessor {
                validate { true }
                process { amount -> amount > 0 }
            }

        // When/Then
        assertTrue(processor.validate("any"))
        assertTrue(processor.process(100.0))

        // These should still error
        assertFailsWith<IllegalStateException> { processor.refund("tx") }
        assertFailsWith<IllegalStateException> { processor.getFee(50.0) }
    }

    @Test
    fun `GIVEN abstract class WHEN methods configured independently THEN should not interfere`() {
        // Given
        var processCount = 0
        var validateCount = 0

        val processor =
            fakePaymentProcessor {
                process { amount ->
                    processCount++
                    true
                }
                validate { card ->
                    validateCount++
                    true
                }
                refund { "0.0".toDouble() }
                getFee { 0.0 }
            }

        // When
        processor.process(100.0)
        processor.process(200.0)
        processor.validate("1234")

        // Then
        assertEquals(2, processCount, "process called twice")
        assertEquals(1, validateCount, "validate called once")
    }
}
