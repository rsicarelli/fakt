// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.stdlibTypes

import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.stdlibTypes.SequenceMapper
import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.stdlibTypes.fakeSequenceMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SequenceMapper SAM interface.
 */
class SequenceMapperTest {
    @Test
    fun `GIVEN SequenceMapper SAM WHEN mapping sequence THEN should lazily transform elements`() {
        // Given
        val mapper =
            fakeSequenceMapper<Int, String> {
                map { sequence -> sequence.map { it.toString() } }
            }

        // When
        val result = mapper.map(sequenceOf(1, 2, 3))

        // Then
        assertEquals(listOf("1", "2", "3"), result.toList(), "Should map sequence elements")
    }

    @Test
    fun `GIVEN SequenceMapper SAM WHEN mapping empty sequence THEN should return empty sequence`() {
        // Given
        val mapper =
            fakeSequenceMapper<String, Int> {
                map { sequence -> sequence.map { it.length } }
            }

        // When
        val result = mapper.map(emptySequence())

        // Then
        assertTrue(result.toList().isEmpty(), "Should handle empty sequence")
    }

    @Test
    fun `GIVEN SequenceMapper SAM WHEN filtering sequence THEN should preserve laziness`() {
        // Given
        var transformCount = 0
        val mapper =
            fakeSequenceMapper<Int, Int> {
                map { sequence ->
                    sequence.map {
                        transformCount++
                        it * 2
                    }
                }
            }

        // When
        val result = mapper.map(sequenceOf(1, 2, 3, 4, 5))

        // Then - sequence not evaluated yet
        assertEquals(0, transformCount, "Should not transform until consumed")

        // When - take first 2 elements
        result.take(2).toList()

        // Then - only 2 elements transformed
        assertEquals(2, transformCount, "Should only transform consumed elements")
    }
}
