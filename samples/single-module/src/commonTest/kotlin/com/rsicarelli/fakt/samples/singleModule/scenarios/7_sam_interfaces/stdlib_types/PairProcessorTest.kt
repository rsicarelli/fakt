// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for PairProcessor SAM interface.
 */
class PairProcessorTest {
    @Test
    fun `GIVEN PairProcessor SAM WHEN processing pair THEN should return processed result`() {
        // Given
        val processor =
            fakePairProcessor<Int> {
                process { pair -> pair.first + pair.second }
            }

        // When
        val result = processor.process(Pair(10, 32))

        // Then
        assertEquals(42, result)
    }
}
