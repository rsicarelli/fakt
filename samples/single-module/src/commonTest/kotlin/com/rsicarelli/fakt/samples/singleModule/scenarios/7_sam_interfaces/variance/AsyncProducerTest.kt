// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for AsyncProducer SAM interface.
 */
class AsyncProducerTest {
    @Test
    fun `GIVEN AsyncProducer SAM WHEN producing with transform THEN should work with suspend`() =
        runTest {
            // Given
            val producer =
                fakeAsyncProducer<String> {
                    produce { "value" }
                }

            // When
            val result = producer.produce()

            // Then
            assertEquals("value", result, "Should produce async value")
        }

    @Test
    fun `GIVEN AsyncProducer SAM WHEN transform is suspend THEN should handle coroutines`() =
        runTest {
            // Given
            val producer =
                fakeAsyncProducer<Int> {
                    produce { 42 }
                }

            // When
            val result = producer.produce()

            // Then
            assertEquals(42, result, "Should apply transform to produced value")
        }
}
