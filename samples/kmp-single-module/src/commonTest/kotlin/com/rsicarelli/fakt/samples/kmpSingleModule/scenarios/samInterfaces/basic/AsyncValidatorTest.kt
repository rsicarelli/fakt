// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for AsyncValidator SAM interface.
 */
class AsyncValidatorTest {
    @Test
    fun `GIVEN SAM with suspend function WHEN using fake THEN should support coroutines`() =
        runTest {
            // Given
            val validator =
                fakeAsyncValidator {
                    validate { input -> input.length > 3 }
                }

            // When
            val shortResult = validator.validate("ab")
            val longResult = validator.validate("hello")

            // Then
            assertFalse(shortResult, "Should reject short strings")
            assertTrue(longResult, "Should accept long strings")
        }
}
