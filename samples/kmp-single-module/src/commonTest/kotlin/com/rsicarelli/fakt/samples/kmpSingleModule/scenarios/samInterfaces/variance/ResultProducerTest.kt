// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ResultProducer SAM interface.
 */
class ResultProducerTest {
    @Test
    fun `GIVEN ResultProducer SAM WHEN producing result THEN should return result`() {
        // Given
        val producer =
            fakeResultProducer<String> {
                produce { Result.success("success-value") }
            }

        // When
        val result = producer.produce()

        // Then
        assertEquals(Result.success("success-value"), result)
    }
}
