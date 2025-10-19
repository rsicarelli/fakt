// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.stdlibTypes

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for TripleAggregator SAM interface.
 */
class TripleAggregatorTest {
    @Test
    fun `GIVEN TripleAggregator SAM WHEN aggregating triple THEN should combine values`() {
        // Given
        val aggregator =
            fakeTripleAggregator<Int> {
                aggregate { triple -> triple.first + triple.second + triple.third }
            }

        // When
        val result = aggregator.aggregate(Triple(1, 2, 3))

        // Then
        assertEquals(6, result)
    }
}
