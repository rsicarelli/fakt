// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeArrayHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeMapProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeSetTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeListMapper
import org.junit.experimental.runners.Enclosed
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 4: SAM interfaces with collection generics (List, Set, Map, Array).
 *
 * Tests cover:
 * - List<T> transformations
 * - Set<T> operations
 * - Map<K, V> processing
 * - Array<T> handling
 */
@RunWith(Enclosed::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SAMCollectionsTest {

    @Nested
    inner class ListMapperTests {
        @Test
        fun `GIVEN ListMapper SAM WHEN mapping list elements THEN should transform all items`() {
            // Given
            val mapper = fakeListMapper<Int, String> {
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
            val mapper = fakeListMapper<Int, String> {
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
            val mapper = fakeListMapper<Int?, String?> {
                map { items -> items.map { it?.toString() } }
            }

            // When
            val result = mapper.map(listOf(1, null, 3))

            // Then
            assertEquals(listOf("1", null, "3"), result, "Should handle nullable list elements")
        }
    }

    @Nested
    inner class SetTransformerTests {
        @Test
        fun `GIVEN SetTransformer SAM WHEN transforming set THEN should preserve uniqueness`() {
            // Given
            val transformer = fakeSetTransformer<Int> {
                transform { items -> items.map { it * 2 }.toSet() }
            }

            // When
            val result = transformer.transform(setOf(1, 2, 3))

            // Then
            assertEquals(setOf(2, 4, 6), result, "Should transform set elements")
        }

        @Test
        fun `GIVEN SetTransformer SAM WHEN transforming empty set THEN should return empty set`() {
            // Given
            val transformer = fakeSetTransformer<String> {
                transform { items -> items.map { it.uppercase() }.toSet() }
            }

            // When
            val result = transformer.transform(emptySet())

            // Then
            assertTrue(result.isEmpty(), "Should handle empty set")
        }
    }

    @Nested
    inner class MapProcessorTests {
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
            assertEquals(mapOf("nums" to listOf("1", "2", "3")), result, "Should handle nested collections")
        }
    }

    @Nested
    inner class ArrayHandlerTests {
        @Test
        fun `GIVEN ArrayHandler SAM WHEN handling array THEN should process all elements`() {
            // Given
            val handler = fakeArrayHandler<String> {
                handle { items -> items.map { it.uppercase() }.toTypedArray() }
            }

            // When
            val result = handler.handle(arrayOf("a", "b", "c"))

            // Then
            assertContentEquals(arrayOf("A", "B", "C"), result, "Should handle array elements")
        }

        @Test
        fun `GIVEN ArrayHandler SAM WHEN handling empty array THEN should return empty array`() {
            // Given
            val handler = fakeArrayHandler<Int> {
                handle { items -> items.map { it * 2 }.toTypedArray() }
            }

            // When
            val result = handler.handle(emptyArray())

            // Then
            assertEquals(0, result.size, "Should handle empty array")
        }
    }
}
