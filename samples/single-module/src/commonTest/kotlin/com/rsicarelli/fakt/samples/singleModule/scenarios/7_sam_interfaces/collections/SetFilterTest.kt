// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.collections.SetFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.collections.fakeSetFilter
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for SetFilter SAM interface.
 */
class SetFilterTest {
    @Test
    fun `GIVEN SetFilter SAM WHEN filtering set THEN should return filtered set`() {
        // Given
        val filter =
            fakeSetFilter<Int> {
                filter { set -> set.filter { it > 3 }.toSet() }
            }

        // When
        val result = filter.filter(setOf(1, 2, 3, 4, 5))

        // Then
        assertEquals(setOf(4, 5), result)
    }
}
