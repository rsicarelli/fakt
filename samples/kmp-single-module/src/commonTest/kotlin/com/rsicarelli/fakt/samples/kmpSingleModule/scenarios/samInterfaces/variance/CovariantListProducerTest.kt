// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for CovariantListProducer SAM interface.
 */
class CovariantListProducerTest {
    @Test
    fun `GIVEN CovariantListProducer SAM WHEN producing list THEN should work with out variance`() {
        // Given
        val producer =
            fakeCovariantListProducer<String> {
                produce { listOf("a", "b", "c") }
            }

        // When
        val result = producer.produce()

        // Then
        assertEquals(listOf("a", "b", "c"), result, "Should produce list")
    }

    @Test
    fun `GIVEN CovariantListProducer SAM WHEN producing empty list THEN should handle correctly`() {
        // Given
        val producer =
            fakeCovariantListProducer<Int> {
                produce { emptyList() }
            }

        // When
        val result = producer.produce()

        // Then
        assertTrue(result.isEmpty(), "Should produce empty list")
    }
}
