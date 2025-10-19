// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.stdlibTypes

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for PairMapper SAM interface.
 */
class PairMapperTest {
    @Test
    fun `GIVEN PairMapper SAM WHEN mapping pair THEN should transform both elements`() {
        // Given
        val mapper =
            fakePairMapper<Int, String, String, Int> {
                map { pair -> pair.second to pair.first.toString().length }
            }

        // When
        val result = mapper.map(42 to "hello")

        // Then
        assertEquals("hello" to 2, result, "Should swap and transform pair")
    }

    @Test
    fun `GIVEN PairMapper SAM WHEN mapping with same types THEN should work correctly`() {
        // Given
        val mapper =
            fakePairMapper<String, String, String, String> {
                map { pair -> pair.first.uppercase() to pair.second.lowercase() }
            }

        // When
        val result = mapper.map("Hello" to "WORLD")

        // Then
        assertEquals("HELLO" to "world", result, "Should transform both elements")
    }
}
