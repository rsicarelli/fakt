// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.PredicateCombiner
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakePredicateCombiner
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for PredicateCombiner SAM interface.
 */
class PredicateCombinerTest {
    @Test
    fun `GIVEN PredicateCombiner SAM WHEN combining THEN should combine predicates`() {
        // Given
        val combiner =
            fakePredicateCombiner<Int> {
                combine { p1, p2 -> { x -> p1(x) && p2(x) } }
            }

        // When
        val combined = combiner.combine({ it > 5 }, { it < 10 })

        // Then
        assertTrue(combined(7))
    }
}
