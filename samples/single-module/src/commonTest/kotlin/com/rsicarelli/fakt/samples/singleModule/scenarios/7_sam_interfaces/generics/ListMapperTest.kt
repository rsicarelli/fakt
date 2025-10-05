// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.generics.ListMapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.generics.fakeListMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ListMapper SAM interface.
 */
class ListMapperTest {
    @Test
    fun `GIVEN ListMapper SAM WHEN mapping list elements THEN should transform all items`() {
        // Given
        val mapper =
            fakeListMapper<Int, String> {
                map { items -> items.map { it.toString() } }
            }

        // When
        val result = mapper.map(listOf(1, 2, 3))

        // Then
        assertEquals(listOf("1", "2", "3"), result, "Should map all list elements")
    }

    @Test
    fun `GIVEN ListMapper SAM WHEN mapping empty list THEN should return empty list`() {
        // Given
        val mapper =
            fakeListMapper<Int, String> {
                map { items -> items.map { it.toString() } }
            }

        // When
        val result = mapper.map(emptyList())

        // Then
        assertTrue(result.isEmpty(), "Should handle empty list")
    }

    @Test
    fun `GIVEN ListMapper SAM with nullable elements WHEN mapping THEN should handle nulls`() {
        // Given
        val mapper =
            fakeListMapper<Int?, String?> {
                map { items -> items.map { it?.toString() } }
            }

        // When
        val result = mapper.map(listOf(1, null, 3))

        // Then
        assertEquals(listOf("1", null, "3"), result, "Should handle nullable list elements")
    }
}
