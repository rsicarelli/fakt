// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ContravariantConsumer SAM interface.
 */
class ContravariantConsumerTest {
    @Test
    fun `GIVEN ContravariantConsumer SAM WHEN consuming value THEN should accept supertypes`() {
        // Given
        val consumed = mutableListOf<String>()
        val consumer =
            fakeContravariantConsumer<String> {
                consume { value -> consumed.add(value) }
            }

        // When
        consumer.consume("test")

        // Then
        assertEquals(listOf("test"), consumed, "Should consume value")
    }

    @Test
    fun `GIVEN ContravariantConsumer SAM WHEN consuming multiple THEN should accumulate`() {
        // Given
        val consumed = mutableListOf<Int>()
        val consumer =
            fakeContravariantConsumer<Int> {
                consume { value -> consumed.add(value) }
            }

        // When
        consumer.consume(1)
        consumer.consume(2)
        consumer.consume(3)

        // Then
        assertEquals(listOf(1, 2, 3), consumed, "Should consume all values")
    }
}
