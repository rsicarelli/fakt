// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces
import org.junit.experimental.runners.Enclosed

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.CollectionFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeCollectionFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.IterableProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeIterableProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.ListProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeListProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.MapTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeMapTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.MapWithFunction
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeMapWithFunction
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.MutableListHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeMutableListHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.NestedCollectionHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeNestedCollectionHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.SetFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeSetFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.ArrayProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeArrayProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.RecursiveComparator
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases.fakeRecursiveComparator
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.Transformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.ActionWrapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakeActionWrapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.FunctionComposer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakeFunctionComposer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.PredicateCombiner
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakePredicateCombiner
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.ResultFunctionMapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakeResultFunctionMapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.SuspendExecutor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order.fakeSuspendExecutor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.ErrorHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.fakeErrorHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.LazyProvider
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.fakeLazyProvider
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.PairProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.fakePairProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.ResultProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.fakeResultProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.SequenceFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.fakeSequenceFilter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.TripleAggregator
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.fakeTripleAggregator
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.Consumer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.fakeConsumer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.ListConsumer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.fakeListConsumer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.Producer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.fakeProducer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.ResultProducer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.fakeResultProducer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.VariantTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance.fakeVariantTransformer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for remaining untested SAM interfaces (26 total).
 *
 * Ensures 100% test coverage for all generated fakes.
 */
@RunWith(Enclosed::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SAMRemainingTest {

    @Nested
    inner class ActionWrapperTests {
        @Test
        fun `GIVEN ActionWrapper SAM WHEN wrapping action THEN should execute wrapped action`() {
            // Given
            var executionLog = mutableListOf<String>()
            val wrapper = fakeActionWrapper<String> {
                wrap { action ->
                    { input: String ->
                        executionLog.add("before-$input")
                        action(input)
                        executionLog.add("after-$input")
                    }
                }
            }

            // When
            var executed = ""
            val wrappedAction = wrapper.wrap { value -> executed = value }
            wrappedAction("test")

            // Then
            assertEquals("test", executed)
            assertEquals(listOf("before-test", "after-test"), executionLog)
        }
    }

    @Nested
    inner class ArrayProcessorTests {
        @Test
        fun `GIVEN ArrayProcessor SAM WHEN processing array THEN should return processed array`() {
            // Given
            val processor = fakeArrayProcessor<Int> {
                process { items -> items.map { it * 2 }.toTypedArray() }
            }

            // When
            val result = processor.process(arrayOf(1, 2, 3))

            // Then
            assertEquals(listOf(2, 4, 6), result.toList())
        }
    }

    @Nested
    inner class CollectionFilterTests {
        @Test
        fun `GIVEN CollectionFilter SAM WHEN filtering THEN should return filtered collection`() {
            // Given
            val filter = fakeCollectionFilter<Int> {
                filter { items, predicate -> items.filter(predicate) }
            }

            // When
            val result = filter.filter(listOf(1, 6, 3, 8, 2, 9)) { it > 5 }

            // Then
            assertEquals(listOf(6, 8, 9), result)
        }
    }

    @Nested
    inner class ConsumerTests {
        @Test
        fun `GIVEN Consumer SAM WHEN consuming THEN should consume value`() {
            // Given
            var consumed = ""
            val consumer = fakeConsumer<String> {
                consume { value -> consumed = value }
            }

            // When
            consumer.consume("test-value")

            // Then
            assertEquals("test-value", consumed)
        }
    }

    @Nested
    inner class ProducerTests {
        @Test
        fun `GIVEN Producer SAM WHEN producing THEN should return value`() {
            // Given
            val producer = fakeProducer<String> {
                produce { "produced-value" }
            }

            // When
            val result = producer.produce()

            // Then
            assertEquals("produced-value", result)
        }
    }

    @Nested
    inner class ErrorHandlerTests {
        @Test
        fun `GIVEN ErrorHandler SAM WHEN handling error THEN should return handled result`() {
            // Given
            val handler = fakeErrorHandler<String> {
                handle { result -> result.getOrNull() ?: "default" }
            }

            // When
            val successResult = handler.handle(Result.success("success"))
            val failureResult = handler.handle(Result.failure(Exception("error")))

            // Then
            assertEquals("success", successResult)
            assertEquals("default", failureResult)
        }
    }

    @Nested
    inner class FunctionComposerTests {
        @Test
        fun `GIVEN FunctionComposer SAM WHEN composing THEN should compose functions`() {
            // Given
            val composer = fakeFunctionComposer<Int, String, Int> {
                compose { fn1, fn2, input -> fn2(fn1(input)) }
            }

            // When
            val result = composer.compose({ (it * 2).toString() }, { it.length }, 5)

            // Then
            assertEquals(2, result)
        }
    }

    @Nested
    inner class PredicateCombinerTests {
        @Test
        fun `GIVEN PredicateCombiner SAM WHEN combining THEN should combine predicates`() {
            // Given
            val combiner = fakePredicateCombiner<Int> {
                combine { p1, p2 -> { x -> p1(x) && p2(x) } }
            }

            // When
            val combined = combiner.combine({ it > 5 }, { it < 10 })

            // Then
            assertTrue(combined(7))
        }
    }

    @Nested
    inner class VariantTransformerTests {
        @Test
        fun `GIVEN VariantTransformer SAM WHEN transforming THEN should transform with variance`() {
            // Given
            val transformer = fakeVariantTransformer<String, Int> {
                transform { input -> input.length }
            }

            // When
            val result = transformer.transform("hello")

            // Then
            assertEquals(5, result)
        }
    }

    @Nested
    inner class ListConsumerTests {
        @Test
        fun `GIVEN ListConsumer SAM WHEN consuming list THEN should accept list`() {
            // Given
            var consumed = listOf<String>()
            val consumer = fakeListConsumer<String> {
                consume { list -> consumed = list }
            }

            // When
            consumer.consume(listOf("a", "b"))

            // Then
            assertEquals(listOf("a", "b"), consumed)
        }
    }

    @Nested
    inner class ListProcessorTests {
        @Test
        fun `GIVEN ListProcessor SAM WHEN processing THEN should return processed list`() {
            // Given
            val processor = fakeListProcessor<Int> {
                process { list -> list.map { it * 2 } }
            }

            // When
            val result = processor.process(listOf(1, 2, 3))

            // Then
            assertEquals(listOf(2, 4, 6), result)
        }
    }

    @Nested
    inner class MapTransformerTests {
        @Test
        fun `GIVEN MapTransformer SAM WHEN transforming map THEN should return map with string values`() {
            // Given
            val transformer = fakeMapTransformer<String, Int> {
                transform { map -> map.mapValues { it.value.toString() } }
            }

            // When
            val result = transformer.transform(mapOf("a" to 1, "b" to 2))

            // Then
            assertEquals(mapOf("a" to "1", "b" to "2"), result)
        }
    }

    @Nested
    inner class MapWithFunctionTests {
        @Test
        fun `GIVEN MapWithFunction SAM WHEN transforming with mapper THEN should apply function`() {
            // Given
            val mapper = fakeMapWithFunction<Int, String> {
                transform { items, fn -> items.map(fn) }
            }

            // When
            val result = mapper.transform(listOf(1, 2, 3)) { (it * 10).toString() }

            // Then
            assertEquals(listOf("10", "20", "30"), result)
        }
    }

    @Nested
    inner class IterableProcessorTests {
        @Test
        fun `GIVEN IterableProcessor SAM WHEN processing iterable THEN should return processed iterable`() {
            // Given
            val processor = fakeIterableProcessor<Int> {
                process { iterable -> iterable.map { it + 1 }.asIterable() }
            }

            // When
            val result = processor.process(listOf(1, 2, 3))

            // Then
            assertEquals(listOf(2, 3, 4), result.toList())
        }
    }

    @Nested
    inner class LazyProviderTests {
        @Test
        fun `GIVEN LazyProvider SAM WHEN providing lazily THEN should return lazy value`() {
            // Given
            val provider = fakeLazyProvider<String> {
                provide { lazy { "lazy-value" } }
            }

            // When
            val result = provider.provide()

            // Then
            assertEquals("lazy-value", result.value)
        }
    }

    @Nested
    inner class MutableListHandlerTests {
        @Test
        fun `GIVEN MutableListHandler SAM WHEN handling THEN should modify list`() {
            // Given
            val handler = fakeMutableListHandler<Int> {
                handle { list -> list.add(99); list }
            }

            // When
            val list = mutableListOf(1, 2, 3)
            val result = handler.handle(list)

            // Then
            assertEquals(listOf(1, 2, 3, 99), result)
        }
    }

    @Nested
    inner class NestedCollectionHandlerTests {
        @Test
        fun `GIVEN NestedCollectionHandler SAM WHEN handling nested collections THEN should transform`() {
            // Given
            val handler = fakeNestedCollectionHandler {
                handle { data ->
                    data.flatMap { map ->
                        map.entries.map { it.key to it.value.toList() }
                    }.toMap()
                }
            }

            // When
            val input = listOf(
                mapOf("a" to setOf(1, 2)),
                mapOf("b" to setOf(3, 4))
            )
            val result = handler.handle(input)

            // Then
            assertEquals(mapOf("a" to listOf(1, 2), "b" to listOf(3, 4)), result)
        }
    }

    @Nested
    inner class PairProcessorTests {
        @Test
        fun `GIVEN PairProcessor SAM WHEN processing pair THEN should return processed result`() {
            // Given
            val processor = fakePairProcessor<Int> {
                process { pair -> pair.first + pair.second }
            }

            // When
            val result = processor.process(Pair(10, 32))

            // Then
            assertEquals(42, result)
        }
    }

    @Nested
    inner class RecursiveComparatorTests {
        @Test
        fun `GIVEN RecursiveComparator SAM WHEN comparing THEN should compare recursively`() {
            // Given
            val comparator = fakeRecursiveComparator<String> {
                compare { a, b -> a.length.compareTo(b.length) }
            }

            // When
            val result = comparator.compare("short", "very long string")

            // Then
            assertTrue(result < 0)
        }
    }

    @Nested
    inner class SequenceFilterTests {
        @Test
        fun `GIVEN SequenceFilter SAM WHEN filtering sequence THEN should return filtered sequence`() {
            // Given
            val filter = fakeSequenceFilter<Int> {
                filter { seq, predicate -> seq.filter(predicate) }
            }

            // When
            val result = filter.filter(sequenceOf(1, 2, 3, 4, 5)) { it % 2 == 0 }

            // Then
            assertEquals(listOf(2, 4), result.toList())
        }
    }

    @Nested
    inner class SetFilterTests {
        @Test
        fun `GIVEN SetFilter SAM WHEN filtering set THEN should return filtered set`() {
            // Given
            val filter = fakeSetFilter<Int> {
                filter { set -> set.filter { it > 3 }.toSet() }
            }

            // When
            val result = filter.filter(setOf(1, 2, 3, 4, 5))

            // Then
            assertEquals(setOf(4, 5), result)
        }
    }

    @Nested
    inner class ResultFunctionMapperTests {
        @Test
        fun `GIVEN ResultFunctionMapper SAM WHEN mapping result function THEN should transform`() {
            // Given
            val mapper = fakeResultFunctionMapper<Int, String> {
                mapResult { fn, input -> Result.success(fn(input)) }
            }

            // When
            val result = mapper.mapResult({ it.toString() }, 42)

            // Then
            assertEquals(Result.success("42"), result)
        }
    }

    @Nested
    inner class ResultProcessorTests {
        @Test
        fun `GIVEN ResultProcessor SAM WHEN processing input THEN should return result`() {
            // Given
            val processor = fakeResultProcessor<Int> {
                process { input -> Result.success(input * 2) }
            }

            // When
            val result = processor.process(21)

            // Then
            assertEquals(Result.success(42), result)
        }
    }

    @Nested
    inner class ResultProducerTests {
        @Test
        fun `GIVEN ResultProducer SAM WHEN producing result THEN should return result`() {
            // Given
            val producer = fakeResultProducer<String> {
                produce { Result.success("success-value") }
            }

            // When
            val result = producer.produce()

            // Then
            assertEquals(Result.success("success-value"), result)
        }
    }

    @Nested
    inner class SuspendExecutorTests {
        @Test
        fun `GIVEN SuspendExecutor SAM WHEN executing suspend function THEN should execute`() = kotlinx.coroutines.test.runTest {
            // Given
            val executor = fakeSuspendExecutor<Int, String> {
                execute { fn, input -> fn(input) }
            }

            // When
            val result = executor.execute({ input -> input.toString() }, 42)

            // Then
            assertEquals("42", result)
        }
    }

    @Nested
    inner class TripleAggregatorTests {
        @Test
        fun `GIVEN TripleAggregator SAM WHEN aggregating triple THEN should combine values`() {
            // Given
            val aggregator = fakeTripleAggregator<Int> {
                aggregate { triple -> triple.first + triple.second + triple.third }
            }

            // When
            val result = aggregator.aggregate(Triple(1, 2, 3))

            // Then
            assertEquals(6, result)
        }
    }
}
