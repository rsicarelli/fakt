// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ListConsumer SAM interface.
 */
class ListConsumerTest {
    @Test
    fun `GIVEN ListConsumer SAM WHEN consuming list THEN should accept list`() {
        // Given
        var consumed = listOf<String>()
        val consumer =
            fakeListConsumer<String> {
                consume { list -> consumed = list }
            }

        // When
        consumer.consume(listOf("a", "b"))

        // Then
        assertEquals(listOf("a", "b"), consumed)
    }
}
