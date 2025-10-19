// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections.SetTransformer
import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections.fakeSetTransformer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SetTransformer SAM interface.
 */
class SetTransformerTest {
    @Test
    fun `GIVEN SetTransformer SAM WHEN transforming set THEN should preserve uniqueness`() {
        // Given
        val transformer =
            fakeSetTransformer<Int> {
                transform { items -> items.map { it * 2 }.toSet() }
            }

        // When
        val result = transformer.transform(setOf(1, 2, 3))

        // Then
        assertEquals(setOf(2, 4, 6), result, "Should transform set elements")
    }

    @Test
    fun `GIVEN SetTransformer SAM WHEN transforming empty set THEN should return empty set`() {
        // Given
        val transformer =
            fakeSetTransformer<String> {
                transform { items -> items.map { it.uppercase() }.toSet() }
            }

        // When
        val result = transformer.transform(emptySet())

        // Then
        assertTrue(result.isEmpty(), "Should handle empty set")
    }
}
