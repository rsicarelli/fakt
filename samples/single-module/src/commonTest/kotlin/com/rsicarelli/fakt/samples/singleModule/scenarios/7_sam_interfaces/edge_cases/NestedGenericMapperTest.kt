// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.NestedGenericMapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeNestedGenericMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for NestedGenericMapper SAM interface.
 */
class NestedGenericMapperTest {
    @Test
    fun `GIVEN NestedGenericMapper SAM WHEN mapping nested collections THEN should transform deeply`() {
        // Given
        val mapper =
            fakeNestedGenericMapper<Int, String> {
                map { nested -> nested.map { list -> list.map { it.toString() } } }
            }

        // When
        val result = mapper.map(listOf(listOf(1, 2), listOf(3, 4)))

        // Then
        assertEquals(
            listOf(listOf("1", "2"), listOf("3", "4")),
            result,
            "Should map nested collections",
        )
    }

    @Test
    fun `GIVEN NestedGenericMapper SAM WHEN mapping empty nested THEN should handle correctly`() {
        // Given
        val mapper =
            fakeNestedGenericMapper<String, Int> {
                map { nested -> nested.map { list -> list.map { it.length } } }
            }

        // When
        val result = mapper.map(emptyList())

        // Then
        assertTrue(result.isEmpty(), "Should handle empty nested collection")
    }
}
