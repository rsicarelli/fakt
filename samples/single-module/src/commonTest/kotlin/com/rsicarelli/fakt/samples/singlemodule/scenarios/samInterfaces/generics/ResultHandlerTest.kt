// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.generics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ResultHandler SAM interface.
 */
class ResultHandlerTest {
    @Test
    fun `GIVEN SAM with Result generic WHEN handling THEN should wrap in Result`() {
        // Given
        val handler =
            fakeResultHandler<String> {
                handle { input -> Result.success(input.uppercase()) }
            }

        // When
        val result = handler.handle("hello")

        // Then
        assertTrue(result.isSuccess, "Should return success")
        assertEquals("HELLO", result.getOrNull(), "Should contain transformed value")
    }

    @Test
    fun `GIVEN ResultHandler SAM WHEN handling success THEN should wrap in Result success`() {
        // Given
        val handler =
            fakeResultHandler<String> {
                handle { input -> Result.success(input.uppercase()) }
            }

        // When
        val result = handler.handle("hello")

        // Then
        assertTrue(result.isSuccess, "Should return success")
        assertEquals("HELLO", result.getOrNull(), "Should contain transformed value")
    }

    @Test
    fun `GIVEN ResultHandler SAM WHEN handling failure THEN should wrap in Result failure`() {
        // Given
        val handler =
            fakeResultHandler<String> {
                handle { _ -> Result.failure(IllegalStateException("Test error")) }
            }

        // When
        val result = handler.handle("test")

        // Then
        assertTrue(result.isFailure, "Should return failure")
        assertNull(result.getOrNull(), "Should contain no value")
    }
}
