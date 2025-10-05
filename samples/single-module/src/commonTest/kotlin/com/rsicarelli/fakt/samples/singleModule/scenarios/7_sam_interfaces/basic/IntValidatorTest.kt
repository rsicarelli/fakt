// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.basic

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.basic.IntValidator
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.basic.fakeIntValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for IntValidator SAM interface.
 */
class IntValidatorTest {
    @Test
    fun `GIVEN SAM with Int param WHEN configuring fake THEN should validate correctly`() {
        // Given
        val validator =
            fakeIntValidator {
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
    fun `GIVEN SAM fake without configuration WHEN using default THEN should have sensible default`() {
        // Given - no configuration
        val validator = fakeIntValidator()

        // When
        val result = validator.validate(42)

        // Then - should have a default behavior (likely returns false or throws)
        assertFalse(result, "Should have default false behavior for validators")
    }
}
