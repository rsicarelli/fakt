// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.stdlibTypes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultFunctionHandlerTest {
    @Test
    fun `GIVEN ResultFunctionHandler SAM WHEN transforming with Result THEN should handle both types`() {
        // Given
        val handler = fakeResultFunctionHandler<String, Int> {
            handle { result, mapper -> result.map(mapper) }
        }

        // When
        val result = handler.handle(Result.success("hello"), { it.length })

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
    }

    @Test
    fun `GIVEN ResultFunctionHandler SAM WHEN input fails THEN should propagate failure`() {
        // Given
        val handler = fakeResultFunctionHandler<String, Int> {
            handle { result, mapper -> result.map(mapper) }
        }

        // When
        val result =
            handler.handle(Result.failure(IllegalArgumentException("Invalid")), { it.length })

        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
