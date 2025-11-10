// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections.MapProcessor
import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections.fakeMapProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapProcessorTest {
    @Test
    fun `GIVEN MapProcessor SAM WHEN processing map entries THEN should transform all values`() {
        // Given
        val processor = fakeMapProcessor<String, Int, String> {
            process { map -> map.mapValues { it.value.toString() } }
        }

        // When
        val result = processor.process(mapOf("a" to 1, "b" to 2))

        // Then
        assertEquals(mapOf("a" to "1", "b" to "2"), result, "Should process map values")
    }

    @Test
    fun `GIVEN MapProcessor SAM WHEN processing empty map THEN should return empty map`() {
        // Given
        val processor = fakeMapProcessor<String, Int, Int> {
            process { map -> map.mapValues { it.value * 2 } }
        }

        // When
        val result = processor.process(emptyMap())

        // Then
        assertTrue(result.isEmpty(), "Should handle empty map")
    }

    @Test
    fun `GIVEN MapProcessor SAM with complex values WHEN processing THEN should transform nested data`() {
        // Given
        val processor = fakeMapProcessor<String, List<Int>, List<String>> {
            process { map -> map.mapValues { it.value.map { num -> num.toString() } } }
        }

        // When
        val result = processor.process(mapOf("nums" to listOf(1, 2, 3)))

        // Then
        assertEquals(
            mapOf("nums" to listOf("1", "2", "3")),
            result,
            "Should handle nested collections"
        )
    }
}
