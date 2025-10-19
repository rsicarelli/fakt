// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for CollectionFilter SAM interface.
 */
class CollectionFilterTest {
    @Test
    fun `GIVEN CollectionFilter SAM WHEN filtering THEN should return filtered collection`() {
        // Given
        val filter =
            fakeCollectionFilter<Int> {
                filter { items, predicate -> items.filter(predicate) }
            }

        // When
        val result = filter.filter(listOf(1, 6, 3, 8, 2, 9)) { it > 5 }

        // Then
        assertEquals(listOf(6, 8, 9), result)
    }
}
