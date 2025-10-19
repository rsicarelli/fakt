// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for InvariantTransformer SAM interface.
 */
class InvariantTransformerTest {
    @Test
    fun `GIVEN InvariantTransformer SAM WHEN transforming THEN should require exact type`() {
        // Given
        val transformer =
            fakeInvariantTransformer<String> {
                transform { value -> value.uppercase() }
            }

        // When
        val result = transformer.transform("hello")

        // Then
        assertEquals("HELLO", result, "Should transform value")
    }
}
