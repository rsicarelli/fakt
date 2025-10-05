// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.edgeCases

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for RecursiveComparator SAM interface.
 */
class RecursiveComparatorTest {
    @Test
    fun `GIVEN RecursiveComparator SAM WHEN comparing THEN should compare recursively`() {
        // Given
        val comparator =
            fakeRecursiveComparator<String> {
                compare { a, b -> a.length.compareTo(b.length) }
            }

        // When
        val result = comparator.compare("short", "very long string")

        // Then
        assertTrue(result < 0)
    }
}
