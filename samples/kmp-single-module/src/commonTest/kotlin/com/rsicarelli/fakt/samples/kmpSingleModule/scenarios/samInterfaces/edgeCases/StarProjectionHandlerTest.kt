// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.edgeCases

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for StarProjectionHandler SAM interface.
 */
class StarProjectionHandlerTest {
    @Test
    fun `GIVEN StarProjectionHandler SAM WHEN handling star projection THEN should work with any list`() {
        // Given
        val handler =
            fakeStarProjectionHandler {
                handle { items -> items.size }
            }

        // When
        val result = handler.handle(listOf("a", "b", "c"))

        // Then
        assertEquals(3, result, "Should handle star projected list")
    }

    @Test
    fun `GIVEN StarProjectionHandler SAM WHEN handling different types THEN should work generically`() {
        // Given
        val handler =
            fakeStarProjectionHandler {
                handle { items -> items.size }
            }

        // When
        val stringResult = handler.handle(listOf("x", "y"))
        val intResult = handler.handle(listOf(1, 2, 3, 4))

        // Then
        assertEquals(2, stringResult, "Should handle string list")
        assertEquals(4, intResult, "Should handle int list")
    }
}
