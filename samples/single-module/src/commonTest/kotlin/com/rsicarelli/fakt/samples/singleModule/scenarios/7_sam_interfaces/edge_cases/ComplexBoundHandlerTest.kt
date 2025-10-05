// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.edgeCases

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ComplexBoundHandler SAM interface.
 */
class ComplexBoundHandlerTest {
    @Test
    fun `GIVEN ComplexBoundHandler SAM WHEN handling complex bounds THEN should respect constraints`() {
        // Given - String implements both CharSequence and Comparable<String>
        val handler =
            fakeComplexBoundHandler<String> {
                handle { item -> item.length }
            }

        // When
        val result = handler.handle("test")

        // Then
        assertEquals(4, result, "Should handle complex bounded type")
    }
}
