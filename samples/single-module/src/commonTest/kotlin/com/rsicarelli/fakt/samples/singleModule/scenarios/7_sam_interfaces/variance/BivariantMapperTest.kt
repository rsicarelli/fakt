// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for BivariantMapper SAM interface.
 */
class BivariantMapperTest {
    @Test
    fun `GIVEN BivariantMapper SAM WHEN mapping with both variances THEN should work correctly`() {
        // Given
        val mapper =
            fakeBivariantMapper<String, Int> {
                map { input -> input.length }
            }

        // When
        val result = mapper.map("hello")

        // Then
        assertEquals(5, result, "Should map input to output")
    }
}
