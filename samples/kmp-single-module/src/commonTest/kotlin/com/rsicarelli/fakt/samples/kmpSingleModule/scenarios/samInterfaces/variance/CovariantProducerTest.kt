// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for CovariantProducer SAM interface.
 */
class CovariantProducerTest {
    @Test
    fun `GIVEN CovariantProducer SAM WHEN producing value THEN should work with subtypes`() {
        // Given
        val producer =
            fakeCovariantProducer<String> {
                produce { "test value" }
            }

        // When
        val result = producer.produce()

        // Then
        assertEquals("test value", result, "Should produce value")
    }

    @Test
    fun `GIVEN CovariantProducer SAM WHEN producing nullable THEN should handle null`() {
        // Given
        val producer =
            fakeCovariantProducer<String?> {
                produce { null }
            }

        // When
        val result = producer.produce()

        // Then
        assertEquals(null, result, "Should produce null")
    }

    @Test
    fun `GIVEN CovariantProducer with List WHEN producing THEN should work covariantly`() {
        // Given
        val producer =
            fakeCovariantProducer<List<String>> {
                produce { listOf("a", "b", "c") }
            }

        // When
        val result = producer.produce()

        // Then
        assertEquals(listOf("a", "b", "c"), result, "Should produce list")
    }
}
