// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertFalse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance

/**
 * Phase 5: SAM interfaces with Kotlin stdlib types (Result, Pair, Triple, Sequence).
 *
 * Tests cover:
 * - Result<T> handling with success/failure
 * - Pair<A, B> transformations
 * - Triple<A, B, C> processing
 * - Sequence<T> lazy operations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SAMStdlibTypesTest {

    @Nested
    inner class ResultHandlerTests {
        @Test
        fun `GIVEN ResultHandler SAM WHEN handling success THEN should wrap in Result success`() {
            // Given
            val handler = fakeResultHandler<String> {
                handle { input -> Result.success(input.uppercase()) }
            }

            // When
            val result = handler.handle("hello")

            // Then
            assertTrue(result.isSuccess, "Should return success")
            assertEquals("HELLO", result.getOrNull(), "Should contain transformed value")
        }

        @Test
        fun `GIVEN ResultHandler SAM WHEN handling failure THEN should wrap in Result failure`() {
            // Given
            val handler = fakeResultHandler<String> {
                handle { _ -> Result.failure(IllegalStateException("Test error")) }
            }

            // When
            val result = handler.handle("test")

            // Then
            assertTrue(result.isFailure, "Should return failure")
            assertNull(result.getOrNull(), "Should contain no value")
        }

        @Test
        fun `GIVEN ResultHandler SAM WHEN handling exception THEN should catch and wrap`() {
            // Given
            val handler = fakeResultHandler<Int> {
                handle { input ->
                    runCatching {
                        require(input > 0) { "Must be positive" }
                        input * 2
                    }
                }
            }

            // When
            val validResult = handler.handle(5)
            val invalidResult = handler.handle(-1)

            // Then
            assertTrue(validResult.isSuccess, "Should succeed for valid input")
            assertEquals(10, validResult.getOrNull())
            assertTrue(invalidResult.isFailure, "Should fail for invalid input")
        }
    }

    @Nested
    inner class PairMapperTests {
        @Test
        fun `GIVEN PairMapper SAM WHEN mapping pair THEN should transform both elements`() {
            // Given
            val mapper = fakePairMapper<Int, String, String, Int> {
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
            val mapper = fakePairMapper<String, String, String, String> {
                map { pair -> pair.first.uppercase() to pair.second.lowercase() }
            }

            // When
            val result = mapper.map("Hello" to "WORLD")

            // Then
            assertEquals("HELLO" to "world", result, "Should transform both elements")
        }
    }

    @Nested
    inner class TripleProcessorTests {
        @Test
        fun `GIVEN TripleProcessor SAM WHEN processing triple THEN should transform all elements`() {
            // Given
            val processor = fakeTripleProcessor<Int, String, Boolean, String, Int, String> {
                process { triple ->
                    Triple(
                        triple.second,
                        triple.first * 2,
                        triple.third.toString()
                    )
                }
            }

            // When
            val result = processor.process(Triple(5, "test", true))

            // Then
            assertEquals(Triple("test", 10, "true"), result, "Should transform all triple elements")
        }

        @Test
        fun `GIVEN TripleProcessor SAM WHEN processing with nullable THEN should handle nulls`() {
            // Given
            val processor = fakeTripleProcessor<Int?, String?, Boolean?, String, String, String> {
                process { triple ->
                    Triple(
                        triple.first?.toString() ?: "null",
                        triple.second ?: "null",
                        triple.third?.toString() ?: "null"
                    )
                }
            }

            // When
            val result = processor.process(Triple(null, "test", null))

            // Then
            assertEquals(Triple("null", "test", "null"), result, "Should handle nullable elements")
        }
    }

    @Nested
    inner class SequenceMapperTests {
        @Test
        fun `GIVEN SequenceMapper SAM WHEN mapping sequence THEN should lazily transform elements`() {
            // Given
            val mapper = fakeSequenceMapper<Int, String> {
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
            val mapper = fakeSequenceMapper<String, Int> {
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
            val mapper = fakeSequenceMapper<Int, Int> {
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

    @Nested
    inner class ResultFunctionHandlerTests {
        @Test
        fun `GIVEN ResultFunctionHandler SAM WHEN transforming with Result THEN should handle both types`() {
            // Given
            val handler = fakeResultFunctionHandler<String, Int> {
                handle { result, mapper -> result.map(mapper) }
            }

            // When
            val result = handler.handle(Result.success("hello"), { it.length })

            // Then
            assertTrue(result.isSuccess)
            assertEquals(5, result.getOrNull())
        }

        @Test
        fun `GIVEN ResultFunctionHandler SAM WHEN input fails THEN should propagate failure`() {
            // Given
            val handler = fakeResultFunctionHandler<String, Int> {
                handle { result, mapper -> result.map(mapper) }
            }

            // When
            val result = handler.handle(Result.failure(IllegalArgumentException("Invalid")), { it.length })

            // Then
            assertFalse(result.isSuccess)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }
    }
}
