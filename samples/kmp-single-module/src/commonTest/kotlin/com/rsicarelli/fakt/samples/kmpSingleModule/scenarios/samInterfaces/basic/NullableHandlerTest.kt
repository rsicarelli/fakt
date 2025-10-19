// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic

import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic.NullableHandler
import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic.fakeNullableHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for NullableHandler SAM interface.
 */
class NullableHandlerTest {
    @Test
    fun `GIVEN SAM with nullable types WHEN using fake THEN should handle nulls`() {
        // Given
        val handler =
            fakeNullableHandler {
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
    fun `GIVEN SAM with nullable handler WHEN not configured THEN should return input unchanged`() {
        // Given - no configuration (uses identity function as default)
        val handler = fakeNullableHandler()

        // When
        val result = handler.handle("test")

        // Then
        assertEquals("test", result, "Should return input unchanged (identity function)")
    }
}
