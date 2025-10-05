// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ContravariantListConsumer SAM interface.
 */
class ContravariantListConsumerTest {
    @Test
    fun `GIVEN ContravariantListConsumer SAM WHEN consuming list THEN should work with in variance`() {
        // Given
        val consumed = mutableListOf<List<String>>()
        val consumer =
            fakeContravariantListConsumer<String> {
                consume { list -> consumed.add(list) }
            }

        // When
        consumer.consume(listOf("x", "y", "z"))

        // Then
        assertEquals(listOf(listOf("x", "y", "z")), consumed, "Should consume list")
    }

    @Test
    fun `GIVEN ContravariantListConsumer SAM WHEN consuming empty THEN should handle correctly`() {
        // Given
        val consumed = mutableListOf<List<Int>>()
        val consumer =
            fakeContravariantListConsumer<Int> {
                consume { list -> consumed.add(list) }
            }

        // When
        consumer.consume(emptyList())

        // Then
        assertEquals(listOf(emptyList<Int>()), consumed, "Should consume empty list")
    }
}
