// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections.ArrayHandler
import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections.fakeArrayHandler
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Tests for ArrayHandler SAM interface.
 */
class ArrayHandlerTest {
    @Test
    fun `GIVEN ArrayHandler SAM WHEN handling array THEN should process all elements`() {
        // Given
        val handler =
            fakeArrayHandler<String> {
                handle { items -> items.map { it.uppercase() }.toTypedArray() }
            }

        // When
        val result = handler.handle(arrayOf("a", "b", "c"))

        // Then
        assertContentEquals(arrayOf("A", "B", "C"), result, "Should handle array elements")
    }

    @Test
    fun `GIVEN ArrayHandler SAM WHEN handling empty array THEN should return empty array`() {
        // Given
        val handler =
            fakeArrayHandler<Int> {
                handle { items -> items.map { it * 2 }.toTypedArray() }
            }

        // When
        val result = handler.handle(emptyArray())

        // Then
        assertEquals(0, result.size, "Should handle empty array")
    }
}
