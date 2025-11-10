// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.higherOrder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PredicateFilterTest {
    @Test
    fun `GIVEN PredicateFilter SAM WHEN filtering with predicate THEN should apply condition`() {
        // Given
        val filter = fakePredicateFilter<Int> {
            filter { items, predicate -> items.filter(predicate) }
        }

        // When
        val result = filter.filter(listOf(1, 2, 3, 4, 5)) { it % 2 == 0 }

        // Then
        assertEquals(listOf(2, 4), result, "Should filter by predicate")
    }

    @Test
    fun `GIVEN PredicateFilter SAM WHEN no matches THEN should return empty list`() {
        // Given
        val filter = fakePredicateFilter<String> {
            filter { items, predicate -> items.filter(predicate) }
        }

        // When
        val result = filter.filter(listOf("a", "b", "c")) { it.length > 5 }

        // Then
        assertTrue(result.isEmpty(), "Should return empty when no matches")
    }
}
