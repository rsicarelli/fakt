// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance

/**
 * Phase 7: SAM interfaces with variance annotations (in/out).
 *
 * Tests cover:
 * - Covariance (out T): Producer types
 * - Contravariance (in T): Consumer types
 * - Invariance: Both producer and consumer
 * - Variance with complex types
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SAMVarianceTest {

    @Nested
    inner class CovariantProducerTests {
        @Test
        fun `GIVEN CovariantProducer SAM WHEN producing value THEN should work with subtypes`() {
            // Given
            val producer = fakeCovariantProducer<String> {
                produce { "test value" }
            }

            // When
            val result = producer.produce()

            // Then
            assertEquals("test value", result, "Should produce value")
        }

        @Test
        fun `GIVEN CovariantProducer SAM WHEN producing nullable THEN should handle null`() {
            // Given
            val producer = fakeCovariantProducer<String?> {
                produce { null }
            }

            // When
            val result = producer.produce()

            // Then
            assertEquals(null, result, "Should produce null")
        }

        @Test
        fun `GIVEN CovariantProducer with List WHEN producing THEN should work covariantly`() {
            // Given
            val producer = fakeCovariantProducer<List<String>> {
                produce { listOf("a", "b", "c") }
            }

            // When
            val result = producer.produce()

            // Then
            assertEquals(listOf("a", "b", "c"), result, "Should produce list")
        }
    }

    @Nested
    inner class ContravariantConsumerTests {
        @Test
        fun `GIVEN ContravariantConsumer SAM WHEN consuming value THEN should accept supertypes`() {
            // Given
            val consumed = mutableListOf<String>()
            val consumer = fakeContravariantConsumer<String> {
                consume { value -> consumed.add(value) }
            }

            // When
            consumer.consume("test")

            // Then
            assertEquals(listOf("test"), consumed, "Should consume value")
        }

        @Test
        fun `GIVEN ContravariantConsumer SAM WHEN consuming multiple THEN should accumulate`() {
            // Given
            val consumed = mutableListOf<Int>()
            val consumer = fakeContravariantConsumer<Int> {
                consume { value -> consumed.add(value) }
            }

            // When
            consumer.consume(1)
            consumer.consume(2)
            consumer.consume(3)

            // Then
            assertEquals(listOf(1, 2, 3), consumed, "Should consume all values")
        }
    }

    @Nested
    inner class InvariantTransformerTests {
        @Test
        fun `GIVEN InvariantTransformer SAM WHEN transforming THEN should require exact type`() {
            // Given
            val transformer = fakeInvariantTransformer<String> {
                transform { value -> value.uppercase() }
            }

            // When
            val result = transformer.transform("hello")

            // Then
            assertEquals("HELLO", result, "Should transform value")
        }

        @Test
        fun `GIVEN InvariantTransformer SAM WHEN using complex type THEN should maintain invariance`() {
            // Given
            val transformer = fakeInvariantTransformer<List<Int>> {
                transform { list -> list.map { it * 2 } }
            }

            // When
            val result = transformer.transform(listOf(1, 2, 3))

            // Then
            assertEquals(listOf(2, 4, 6), result, "Should transform list")
        }
    }

    @Nested
    inner class CovariantListProducerTests {
        @Test
        fun `GIVEN CovariantListProducer SAM WHEN producing list THEN should work with out variance`() {
            // Given
            val producer = fakeCovariantListProducer<String> {
                produce { listOf("a", "b", "c") }
            }

            // When
            val result = producer.produce()

            // Then
            assertEquals(listOf("a", "b", "c"), result, "Should produce list")
        }

        @Test
        fun `GIVEN CovariantListProducer SAM WHEN producing empty list THEN should handle correctly`() {
            // Given
            val producer = fakeCovariantListProducer<Int> {
                produce { emptyList() }
            }

            // When
            val result = producer.produce()

            // Then
            assertTrue(result.isEmpty(), "Should produce empty list")
        }
    }

    @Nested
    inner class ContravariantListConsumerTests {
        @Test
        fun `GIVEN ContravariantListConsumer SAM WHEN consuming list THEN should work with in variance`() {
            // Given
            val consumed = mutableListOf<List<String>>()
            val consumer = fakeContravariantListConsumer<String> {
                consume { list -> consumed.add(list) }
            }

            // When
            consumer.consume(listOf("x", "y", "z"))

            // Then
            assertEquals(listOf(listOf("x", "y", "z")), consumed, "Should consume list")
        }

        @Test
        fun `GIVEN ContravariantListConsumer SAM WHEN consuming empty THEN should handle correctly`() {
            // Given
            val consumed = mutableListOf<List<Int>>()
            val consumer = fakeContravariantListConsumer<Int> {
                consume { list -> consumed.add(list) }
            }

            // When
            consumer.consume(emptyList())

            // Then
            assertEquals(listOf(emptyList<Int>()), consumed, "Should consume empty list")
        }
    }

    @Nested
    inner class BivariantMapperTests {
        @Test
        fun `GIVEN BivariantMapper SAM WHEN mapping with both variances THEN should work correctly`() {
            // Given
            val mapper = fakeBivariantMapper<String, Int> {
                map { input -> input.length }
            }

            // When
            val result = mapper.map("hello")

            // Then
            assertEquals(5, result, "Should map input to output")
        }

        @Test
        fun `GIVEN BivariantMapper SAM WHEN using complex types THEN should handle variance`() {
            // Given
            val mapper = fakeBivariantMapper<List<String>, Set<Int>> {
                map { list -> list.map { it.length }.toSet() }
            }

            // When
            val result = mapper.map(listOf("a", "bb", "ccc"))

            // Then
            assertEquals(setOf(1, 2, 3), result, "Should map list to set")
        }
    }
}
