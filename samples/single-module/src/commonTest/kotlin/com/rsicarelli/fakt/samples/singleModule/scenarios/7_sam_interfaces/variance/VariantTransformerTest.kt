// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for VariantTransformer SAM interface.
 */
class VariantTransformerTest {
    @Test
    fun `GIVEN VariantTransformer SAM WHEN transforming THEN should transform with variance`() {
        // Given
        val transformer =
            fakeVariantTransformer<String, Int> {
                transform { input -> input.length }
            }

        // When
        val result = transformer.transform("hello")

        // Then
        assertEquals(5, result)
    }
}
