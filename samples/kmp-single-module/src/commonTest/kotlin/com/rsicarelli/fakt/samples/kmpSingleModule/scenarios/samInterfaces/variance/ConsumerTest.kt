// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for Consumer SAM interface.
 */
class ConsumerTest {
    @Test
    fun `GIVEN Consumer SAM WHEN consuming THEN should consume value`() {
        // Given
        var consumed = ""
        val consumer = fakeConsumer<String> {
            consume { value -> consumed = value }
        }

        // When
        consumer.consume("test-value")

        // Then
        assertEquals("test-value", consumed)
    }
}
