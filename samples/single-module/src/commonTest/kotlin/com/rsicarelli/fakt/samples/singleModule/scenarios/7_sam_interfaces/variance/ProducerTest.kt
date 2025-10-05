// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for Producer SAM interface.
 */
class ProducerTest {
    @Test
    fun `GIVEN Producer SAM WHEN producing THEN should return value`() {
        // Given
        val producer =
            fakeProducer<String> {
                produce { "produced-value" }
            }

        // When
        val result = producer.produce()

        // Then
        assertEquals("produced-value", result)
    }
}
