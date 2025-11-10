// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NullableTransformerTest {
    @Test
    fun `GIVEN SAM with nullable generic WHEN transforming null THEN should handle correctly`() {
        // Given
        val transformer = fakeNullableTransformer<String> {
            transform { input -> input?.uppercase() }
        }

        // When
        val nonNullResult = transformer.transform("hello")
        val nullResult = transformer.transform(null)

        // Then
        assertEquals("HELLO", nonNullResult, "Should transform non-null value")
        assertNull(nullResult, "Should return null for null input")
    }

    @Test
    fun `GIVEN NullableTransformer SAM WHEN transforming null THEN should handle gracefully`() {
        // Given
        val transformer = fakeNullableTransformer<String> {
            transform { input -> input?.uppercase() }
        }

        // When
        val nonNullResult = transformer.transform("hello")
        val nullResult = transformer.transform(null)

        // Then
        assertEquals("HELLO", nonNullResult, "Should transform non-null")
        assertEquals(null, nullResult, "Should return null for null input")
    }
}
