// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces
import org.junit.experimental.runners.Enclosed

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.ArrayProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeArrayProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.ComplexBoundHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeComplexBoundHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.IntArrayProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeIntArrayProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.NestedGenericMapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeNestedGenericMapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.RecursiveGeneric
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeRecursiveGeneric
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.StarProjectionHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeStarProjectionHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.VarargsProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeVarargsProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.NullableTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeNullableTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.Transformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeTransformer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 8: SAM interfaces with edge cases.
 *
 * Tests cover:
 * - Varargs parameters
 * - Star projections (*)
 * - Recursive generics
 * - Complex nested types
 *
 * NOTE: Some of these tests may reveal compiler bugs:
 * - Varargs: "Function type parameters cannot have modifiers"
 * - Star projection: "'handle' overrides nothing"
 */
@RunWith(Enclosed::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SAMEdgeCasesTest {

    @Nested
    inner class VarargsProcessorTests {
        @Test
        fun `GIVEN VarargsProcessor SAM WHEN processing varargs THEN should handle variable arguments`() {
            // Given
            val processor = fakeVarargsProcessor {
                process { items -> listOf(items.joinToString(",")) }
            }

            // When
            val result = processor.process("a", "b", "c")

            // Then
            assertEquals(listOf("a,b,c"), result, "Should process all varargs")
        }

        @Test
        fun `GIVEN VarargsProcessor SAM WHEN processing empty varargs THEN should handle empty`() {
            // Given
            val processor = fakeVarargsProcessor {
                process { items -> listOf(items.joinToString(",")) }
            }

            // When
            val result = processor.process()

            // Then
            assertEquals(listOf(""), result, "Should handle empty varargs")
        }

        @Test
        fun `GIVEN VarargsProcessor SAM WHEN processing single item THEN should work`() {
            // Given
            val processor = fakeVarargsProcessor {
                process { items -> listOf(items.joinToString(",")) }
            }

            // When
            val result = processor.process("single")

            // Then
            assertEquals(listOf("single"), result, "Should handle single vararg")
        }
    }

    @Nested
    inner class IntArrayProcessorTests {
        @Test
        fun `GIVEN IntArrayProcessor SAM WHEN processing int array THEN should sum elements`() {
            // Given
            val processor = fakeIntArrayProcessor {
                process { items -> intArrayOf(items.sum()) }
            }

            // When
            val result = processor.process(intArrayOf(1, 2, 3, 4, 5))

            // Then
            assertContentEquals(intArrayOf(15), result, "Should sum all varargs")
        }

        @Test
        fun `GIVEN IntArrayProcessor SAM WHEN processing empty THEN should return zero`() {
            // Given
            val processor = fakeIntArrayProcessor {
                process { items -> intArrayOf(items.sum()) }
            }

            // When
            val result = processor.process(intArrayOf())

            // Then
            assertContentEquals(intArrayOf(0), result, "Should return zero for empty varargs")
        }
    }

    @Nested
    inner class StarProjectionHandlerTests {
        @Test
        fun `GIVEN StarProjectionHandler SAM WHEN handling star projection THEN should work with any list`() {
            // Given
            val handler = fakeStarProjectionHandler {
                handle { items -> items.size }
            }

            // When
            val result = handler.handle(listOf("a", "b", "c"))

            // Then
            assertEquals(3, result, "Should handle star projected list")
        }

        @Test
        fun `GIVEN StarProjectionHandler SAM WHEN handling different types THEN should work generically`() {
            // Given
            val handler = fakeStarProjectionHandler {
                handle { items -> items.size }
            }

            // When
            val stringResult = handler.handle(listOf("x", "y"))
            val intResult = handler.handle(listOf(1, 2, 3, 4))

            // Then
            assertEquals(2, stringResult, "Should handle string list")
            assertEquals(4, intResult, "Should handle int list")
        }
    }

    @Nested
    inner class RecursiveGenericTests {
        @Test
        fun `GIVEN RecursiveGeneric SAM WHEN processing recursive type THEN should handle self-reference`() {
            // Given - create a simple comparable implementation
            data class Node(val value: Int) : Comparable<Node> {
                override fun compareTo(other: Node) = value.compareTo(other.value)
            }

            val processor = fakeRecursiveGeneric<Node> {
                process { item -> item.value * 2 }
            }

            // When
            val result = processor.process(Node(21))

            // Then
            assertEquals(42, result, "Should process recursive generic type")
        }

        @Test
        fun `GIVEN RecursiveGeneric SAM WHEN using String THEN should work with comparable`() {
            // Given
            val processor = fakeRecursiveGeneric<String> {
                process { item -> item.length }
            }

            // When
            val result = processor.process("hello")

            // Then
            assertEquals(5, result, "Should process String as Comparable")
        }
    }

    @Nested
    inner class NestedGenericMapperTests {
        @Test
        fun `GIVEN NestedGenericMapper SAM WHEN mapping nested collections THEN should transform deeply`() {
            // Given
            val mapper = fakeNestedGenericMapper<Int, String> {
                map { nested -> nested.map { list -> list.map { it.toString() } } }
            }

            // When
            val result = mapper.map(listOf(listOf(1, 2), listOf(3, 4)))

            // Then
            assertEquals(
                listOf(listOf("1", "2"), listOf("3", "4")),
                result,
                "Should map nested collections"
            )
        }

        @Test
        fun `GIVEN NestedGenericMapper SAM WHEN mapping empty nested THEN should handle correctly`() {
            // Given
            val mapper = fakeNestedGenericMapper<String, Int> {
                map { nested -> nested.map { list -> list.map { it.length } } }
            }

            // When
            val result = mapper.map(emptyList())

            // Then
            assertTrue(result.isEmpty(), "Should handle empty nested collection")
        }
    }

    @Nested
    inner class ComplexBoundHandlerTests {
        @Test
        fun `GIVEN ComplexBoundHandler SAM WHEN handling complex bounds THEN should respect constraints`() {
            // Given - String implements both CharSequence and Comparable<String>
            val handler = fakeComplexBoundHandler<String> {
                handle { item -> item.length }
            }

            // When
            val result = handler.handle("test")

            // Then
            assertEquals(4, result, "Should handle complex bounded type")
        }
    }

    @Nested
    inner class NullabilityEdgeCaseTests {
        @Test
        fun `GIVEN NullableTransformer SAM WHEN transforming null THEN should handle gracefully`() {
            // Given
            val transformer = fakeNullableTransformer<String> {
                transform { input -> input?.uppercase() }
            }

            // When
            val nonNullResult = transformer.transform("hello")
            val nullResult = transformer.transform(null)

            // Then
            assertEquals("HELLO", nonNullResult, "Should transform non-null")
            assertEquals(null, nullResult, "Should return null for null input")
        }

        @Test
        fun `GIVEN NullableTransformer SAM WHEN transforming with default THEN should use fallback`() {
            // Given
            val transformer = fakeNullableTransformer<Int> {
                transform { input -> input?.let { it * 2 } ?: -1 }
            }

            // When
            val nonNullResult = transformer.transform(21)
            val nullResult = transformer.transform(null)

            // Then
            assertEquals(42, nonNullResult, "Should transform non-null")
            assertEquals(-1, nullResult, "Should use fallback for null")
        }
    }
}
