// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 1: Basic SAM Interface Tests (P0 - Must Work)
 *
 * Tests fundamental SAM interface support with:
 * - Primitive types
 * - Nullable types
 * - Unit returns
 * - Suspend functions
 * - Multiple parameters
 */
class SAMBasicTest {

    @Test
    fun `GIVEN SAM with Int param WHEN configuring fake THEN should validate correctly`() {
        // Given
        val validator = fakeIntValidator {
            validate { value -> value > 0 }
        }

        // When
        val positiveResult = validator.validate(42)
        val negativeResult = validator.validate(-5)
        val zeroResult = validator.validate(0)

        // Then
        assertTrue(positiveResult, "Should validate positive numbers")
        assertFalse(negativeResult, "Should reject negative numbers")
        assertFalse(zeroResult, "Should reject zero")
    }

    @Test
    fun `GIVEN SAM with nullable types WHEN using fake THEN should handle nulls`() {
        // Given
        val handler = fakeNullableHandler {
            handle { input -> input?.uppercase() }
        }

        // When
        val resultWithValue = handler.handle("hello")
        val resultWithNull = handler.handle(null)

        // Then
        assertEquals("HELLO", resultWithValue, "Should uppercase non-null input")
        assertNull(resultWithNull, "Should return null for null input")
    }

    @Test
    fun `GIVEN SAM with Unit return WHEN configuring THEN should execute action`() {
        // Given
        var executedCommand = ""
        val action = fakeVoidAction {
            execute { command -> executedCommand = command }
        }

        // When
        action.execute("test-command")

        // Then
        assertEquals("test-command", executedCommand, "Should execute command")
    }

    @Test
    fun `GIVEN SAM with suspend function WHEN using fake THEN should support coroutines`() = runTest {
        // Given
        val validator = fakeAsyncValidator {
            validate { input -> input.length > 3 }
        }

        // When
        val shortResult = validator.validate("ab")
        val longResult = validator.validate("hello")

        // Then
        assertFalse(shortResult, "Should reject short strings")
        assertTrue(longResult, "Should accept long strings")
    }

    @Test
    fun `GIVEN SAM with multiple parameters WHEN applying function THEN should compute result`() {
        // Given
        val biFunction = fakeBiFunction {
            apply { a, b -> a + b }
        }

        // When
        val sum = biFunction.apply(10, 20)

        // Then
        assertEquals(30, sum, "Should add two integers")
    }

    @Test
    fun `GIVEN SAM with String return WHEN formatting THEN should convert to string`() {
        // Given
        val formatter = fakeStringFormatter {
            format { value -> "Value: $value" }
        }

        // When
        val result = formatter.format(42)

        // Then
        assertEquals("Value: 42", result, "Should format value as string")
    }

    @Test
    fun `GIVEN SAM fake without configuration WHEN using default THEN should have sensible default`() {
        // Given - no configuration
        val validator = fakeIntValidator()

        // When
        val result = validator.validate(42)

        // Then - should have a default behavior (likely returns false or throws)
        assertFalse(result, "Should have default false behavior for validators")
    }

    @Test
    fun `GIVEN SAM with nullable handler WHEN not configured THEN should return null by default`() {
        // Given - no configuration
        val handler = fakeNullableHandler()

        // When
        val result = handler.handle("test")

        // Then
        assertNull(result, "Should return null as default for nullable types")
    }
}
