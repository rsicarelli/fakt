// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces
import kotlinx.coroutines.test.runTest

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.CallbackHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakeCallbackHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.FunctionExecutor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakeFunctionExecutor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.PredicateFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakePredicateFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.TransformChain
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakeTransformChain
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.AsyncProducer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.fakeAsyncProducer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.Producer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.fakeProducer
import org.junit.experimental.runners.Enclosed
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 6: SAM interfaces with higher-order function parameters.
 *
 * Tests cover:
 * - Function types as parameters: (T) -> R
 * - Predicate functions: (T) -> Boolean
 * - Transform chains: (T) -> R with function params
 * - Suspend function combinations
 */
@RunWith(Enclosed::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SAMHigherOrderTest {

    @Nested
    inner class FunctionExecutorTests {
        @Test
        fun `GIVEN FunctionExecutor SAM WHEN executing with transform THEN should apply function`() {
            // Given
            val executor = fakeFunctionExecutor<Int, String> {
                execute { fn, input -> fn(input) }
            }

            // When
            val result = executor.execute({ it.toString() }, 42)

            // Then
            assertEquals("42", result, "Should execute transform function")
        }

        @Test
        fun `GIVEN FunctionExecutor SAM WHEN chaining multiple transforms THEN should compose`() {
            // Given
            val executor = fakeFunctionExecutor<String, Int> {
                execute { fn, input ->
                    val intermediate = input.uppercase()
                    fn(intermediate)
                }
            }

            // When
            val result = executor.execute({ it.length }, "hello")

            // Then
            assertEquals(5, result, "Should chain transformations")
        }
    }

    @Nested
    inner class PredicateFilterTests {
        @Test
        fun `GIVEN PredicateFilter SAM WHEN filtering with predicate THEN should apply condition`() {
            // Given
            val filter = fakePredicateFilter<Int> {
                filter { items, predicate -> items.filter(predicate) }
            }

            // When
            val result = filter.filter(listOf(1, 2, 3, 4, 5)) { it % 2 == 0 }

            // Then
            assertEquals(listOf(2, 4), result, "Should filter by predicate")
        }

        @Test
        fun `GIVEN PredicateFilter SAM WHEN no matches THEN should return empty list`() {
            // Given
            val filter = fakePredicateFilter<String> {
                filter { items, predicate -> items.filter(predicate) }
            }

            // When
            val result = filter.filter(listOf("a", "b", "c")) { it.length > 5 }

            // Then
            assertTrue(result.isEmpty(), "Should return empty when no matches")
        }
    }

    @Nested
    inner class TransformChainTests {
        @Test
        fun `GIVEN TransformChain SAM WHEN applying chain THEN should compose functions`() {
            // Given
            val chain = fakeTransformChain<Int, String, Boolean> {
                chain { input, first, second ->
                    val intermediate = first(input)
                    second(intermediate)
                }
            }

            // When
            val result = chain.chain(
                input = 42,
                first = { it.toString() },
                second = { it.length > 1 }
            )

            // Then
            assertEquals(true, result, "Should chain transformations")
        }

        @Test
        fun `GIVEN TransformChain SAM WHEN using complex transforms THEN should handle all steps`() {
            // Given
            val chain = fakeTransformChain<String, List<Char>, Int> {
                chain { input, first, second ->
                    val chars = first(input)
                    second(chars)
                }
            }

            // When
            val result = chain.chain(
                input = "hello",
                first = { it.toList() },
                second = { it.size }
            )

            // Then
            assertEquals(5, result, "Should process through chain")
        }
    }

    @Nested
    inner class AsyncProducerTests {
        @Test
        fun `GIVEN AsyncProducer SAM WHEN producing with transform THEN should work with suspend`() = runTest {
            // Given
            val producer = fakeAsyncProducer<String> {
                produce { "value" }
            }

            // When
            val result = producer.produce()

            // Then
            assertEquals("value", result, "Should produce async value")
        }

        @Test
        fun `GIVEN AsyncProducer SAM WHEN transform is suspend THEN should handle coroutines`() = runTest {
            // Given
            val producer = fakeAsyncProducer<Int> {
                produce { 42 }
            }

            // When
            val result = producer.produce()

            // Then
            assertEquals(42, result, "Should apply transform to produced value")
        }
    }

    @Nested
    inner class CallbackHandlerTests {
        @Test
        fun `GIVEN CallbackHandler SAM WHEN handling with callbacks THEN should invoke both`() {
            // Given
            val results = mutableListOf<String>()
            val handler = fakeCallbackHandler<Int> {
                handle { value, onSuccess, onError ->
                    if (value > 0) {
                        onSuccess(value.toString())
                    } else {
                        onError(IllegalArgumentException("Negative value"))
                    }
                }
            }

            // When - success case
            handler.handle(
                value = 42,
                onSuccess = { results.add(it) },
                onError = { }
            )

            // Then
            assertEquals(listOf("42"), results, "Should invoke success callback")
        }

        @Test
        fun `GIVEN CallbackHandler SAM WHEN error occurs THEN should invoke error callback`() {
            // Given
            val errors = mutableListOf<Throwable>()
            val handler = fakeCallbackHandler<Int> {
                handle { value, onSuccess, onError ->
                    if (value > 0) {
                        onSuccess(value.toString())
                    } else {
                        onError(IllegalArgumentException("Negative value"))
                    }
                }
            }

            // When - error case
            handler.handle(
                value = -1,
                onSuccess = { },
                onError = { errors.add(it) }
            )

            // Then
            assertEquals(1, errors.size, "Should invoke error callback")
            assertTrue(errors[0] is IllegalArgumentException)
        }
    }
}
