// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.stdlibTypes

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for SequenceFilter SAM interface.
 */
class SequenceFilterTest {
    @Test
    fun `GIVEN SequenceFilter SAM WHEN filtering sequence THEN should return filtered sequence`() {
        // Given
        val filter =
            fakeSequenceFilter<Int> {
                filter { seq, predicate -> seq.filter(predicate) }
            }

        // When
        val result = filter.filter(sequenceOf(1, 2, 3, 4, 5)) { it % 2 == 0 }

        // Then
        assertEquals(listOf(2, 4), result.toList())
    }
}
