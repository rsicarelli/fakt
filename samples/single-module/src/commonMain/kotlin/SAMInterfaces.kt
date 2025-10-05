// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake
import kotlin.Result

// ============================================================================
// Phase 1: Basic SAM Interfaces (P0 - Must Work)
// ============================================================================

/**
 * Scenario 1: Simple SAM with primitives
 */
@Fake
fun interface IntValidator {
    fun validate(value: Int): Boolean
}

/**
 * Scenario 2: SAM with nullable types
 */
@Fake
fun interface NullableHandler {
    fun handle(input: String?): String?
}

/**
 * Scenario 3: SAM with Unit return
 */
@Fake
fun interface VoidAction {
    fun execute(command: String)
}

/**
 * Scenario 4: SAM with suspend function
 */
@Fake
fun interface AsyncValidator {
    suspend fun validate(input: String): Boolean
}

/**
 * Scenario 5: SAM with multiple parameters
 */
@Fake
fun interface BiFunction {
    fun apply(a: Int, b: Int): Int
}

/**
 * Scenario 6: SAM with String return
 */
@Fake
fun interface StringFormatter {
    fun format(value: Any): String
}

// ============================================================================
// Phase 2: SAM with Class-Level Generics (P0 - Must Work)
// ============================================================================

/**
 * Scenario 7: Single type parameter
 */
@Fake
fun interface Transformer<T> {
    fun transform(input: T): T
}

/**
 * Scenario 8: Two type parameters
 */
@Fake
fun interface Converter<T, R> {
    fun convert(input: T): R
}

/**
 * Scenario 9: Generic with constraint
 */
@Fake
fun interface ComparableProcessor<T : Comparable<T>> {
    fun process(item: T): T
}

/**
 * Scenario 10: Generic with multiple constraints
 */
@Fake
fun interface MultiConstraintHandler<T> where T : CharSequence, T : Comparable<T> {
    fun handle(item: T): Int
}

/**
 * Scenario 11: Generic SAM with nullable type
 */
@Fake
fun interface NullableTransformer<T> {
    fun transform(input: T?): T?
}

/**
 * Scenario 12: Generic SAM with List
 */
@Fake
fun interface ListMapper<T, R> {
    fun map(items: List<T>): List<R>
}

/**
 * Scenario 13: Generic SAM with Result
 */
@Fake
fun interface ResultHandler<T> {
    fun handle(input: T): Result<T>
}

/**
 * Scenario 14: Generic SAM with suspend
 */
@Fake
fun interface AsyncTransformer<T> {
    suspend fun transform(input: T): T
}

// ============================================================================
// Phase 3: SAM with Method-Level Generics (P1 - INVALID)
// ============================================================================
// NOTE: Kotlin language constraint - SAM interfaces CANNOT have method-level
// type parameters. This is by design in Kotlin's type system.
// Error: "Functional interface cannot have an abstract method with type parameters"
//
// These scenarios are documented here as KNOWN LIMITATIONS, not bugs.
// Regular interfaces (non-SAM) work fine with method-level generics.
// ============================================================================

// ============================================================================
// Phase 4: SAM with Collections (P1 - Should Work)
// ============================================================================

/**
 * Scenario 21: List processor
 */
@Fake
fun interface ListProcessor<T> {
    fun process(items: List<T>): List<T>
}

/**
 * Scenario 22: Map transformer
 */
@Fake
fun interface MapTransformer<K, V> {
    fun transform(map: Map<K, V>): Map<K, String>
}

/**
 * Scenario 23: Set filter
 */
@Fake
fun interface SetFilter<T> {
    fun filter(items: Set<T>): Set<T>
}

/**
 * Scenario 24: Nested collections
 */
@Fake
fun interface NestedCollectionHandler {
    fun handle(data: List<Map<String, Set<Int>>>): Map<String, List<Int>>
}

/**
 * Scenario 25: MutableList handler
 */
@Fake
fun interface MutableListHandler<T> {
    fun handle(items: MutableList<T>): MutableList<T>
}

/**
 * Scenario 26: Collection with predicate
 */
@Fake
fun interface CollectionFilter<T> {
    fun filter(items: Collection<T>, predicate: (T) -> Boolean): Collection<T>
}

/**
 * Scenario 27: Map with function parameter
 */
@Fake
fun interface MapWithFunction<T, R> {
    fun transform(items: List<T>, mapper: (T) -> R): List<R>
}

/**
 * Scenario 28: Iterable processor
 */
@Fake
fun interface IterableProcessor<T> {
    fun process(items: Iterable<T>): Iterable<T>
}

/**
 * Scenario 28a: Set transformer
 */
@Fake
fun interface SetTransformer<T> {
    fun transform(items: Set<T>): Set<T>
}

/**
 * Scenario 28b: Map processor with three type params
 */
@Fake
fun interface MapProcessor<K, V, R> {
    fun process(map: Map<K, V>): Map<K, R>
}

/**
 * Scenario 28c: Array handler
 */
@Fake
fun interface ArrayHandler<T> {
    fun handle(items: Array<T>): Array<T>
}

// ============================================================================
// Phase 5: SAM with Kotlin Stdlib Types (P1 - Should Work)
// ============================================================================

/**
 * Scenario 29: Result processor
 */
@Fake
fun interface ResultProcessor<T> {
    fun process(input: T): Result<T>
}

/**
 * Scenario 30: Sequence mapper
 */
@Fake
fun interface SequenceMapper<T, R> {
    fun map(sequence: Sequence<T>): Sequence<R>
}

/**
 * Scenario 31: Pair processor
 */
@Fake
fun interface PairProcessor<T> {
    fun process(pair: Pair<T, T>): T
}

/**
 * Scenario 32: Triple aggregator
 */
@Fake
fun interface TripleAggregator<T> {
    fun aggregate(triple: Triple<T, T, T>): T
}

/**
 * Scenario 33: Result with Error handling
 */
@Fake
fun interface ErrorHandler<T> {
    fun handle(result: Result<T>): T?
}

/**
 * Scenario 34: Lazy value provider
 */
@Fake
fun interface LazyProvider<T> {
    fun provide(): Lazy<T>
}

/**
 * Scenario 35: Result with function parameter (using class generics instead of method generics)
 */
@Fake
fun interface ResultFunctionHandler<T, R> {
    fun handle(result: Result<T>, mapper: (T) -> R): Result<R>
}

/**
 * Scenario 36: Sequence with filter
 */
@Fake
fun interface SequenceFilter<T> {
    fun filter(sequence: Sequence<T>, predicate: (T) -> Boolean): Sequence<T>
}

/**
 * Scenario 36b: Pair mapper - transforms Pair<A,B> to Pair<C,D>
 */
@Fake
fun interface PairMapper<A, B, C, D> {
    fun map(pair: Pair<A, B>): Pair<C, D>
}

/**
 * Scenario 36c: Triple processor - transforms Triple<A,B,C> to Triple<D,E,F>
 */
@Fake
fun interface TripleProcessor<A, B, C, D, E, F> {
    fun process(triple: Triple<A, B, C>): Triple<D, E, F>
}

// ============================================================================
// Phase 6: SAM with Higher-Order Functions (P2 - Nice to Have)
// ============================================================================

/**
 * Scenario 37: Function executor
 */
@Fake
fun interface FunctionExecutor<T, R> {
    fun execute(fn: (T) -> R, input: T): R
}

/**
 * Scenario 38: Suspend function executor
 */
@Fake
fun interface SuspendExecutor<T, R> {
    suspend fun execute(fn: suspend (T) -> R, input: T): R
}

/**
 * Scenario 39: Function composer
 */
@Fake
fun interface FunctionComposer<T, U, R> {
    fun compose(fn1: (T) -> U, fn2: (U) -> R, input: T): R
}

/**
 * Scenario 40: Predicate combiner
 */
@Fake
fun interface PredicateCombiner<T> {
    fun combine(p1: (T) -> Boolean, p2: (T) -> Boolean): (T) -> Boolean
}

/**
 * Scenario 41: Action wrapper
 */
@Fake
fun interface ActionWrapper<T> {
    fun wrap(action: (T) -> Unit): (T) -> Unit
}

/**
 * Scenario 42: Result mapper with function
 */
@Fake
fun interface ResultFunctionMapper<T, R> {
    fun mapResult(fn: (T) -> R, input: T): Result<R>
}

/**
 * Scenario 42a: Predicate filter
 */
@Fake
fun interface PredicateFilter<T> {
    fun filter(items: List<T>, predicate: (T) -> Boolean): List<T>
}

/**
 * Scenario 42b: Transform chain - compose two transforms
 */
@Fake
fun interface TransformChain<T, U, R> {
    fun chain(input: T, first: (T) -> U, second: (U) -> R): R
}

/**
 * Scenario 42c: Callback handler with success/error callbacks
 */
@Fake
fun interface CallbackHandler<T> {
    fun handle(value: T, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit)
}

// ============================================================================
// Phase 7: SAM with Variance (P2 - Nice to Have)
// ============================================================================

/**
 * Scenario 43: Covariant SAM (out)
 */
@Fake
fun interface Producer<out T> {
    fun produce(): T
}

/**
 * Scenario 44: Contravariant SAM (in)
 */
@Fake
fun interface Consumer<in T> {
    fun consume(item: T)
}

/**
 * Scenario 45: Mixed variance
 */
@Fake
fun interface VariantTransformer<in T, out R> {
    fun transform(input: T): R
}

/**
 * Scenario 46: Covariant with Result
 */
@Fake
fun interface ResultProducer<out T> {
    fun produce(): Result<T>
}

/**
 * Scenario 47: Contravariant with List
 */
@Fake
fun interface ListConsumer<in T> {
    fun consume(items: List<T>)
}

/**
 * Scenario 48: Variance with suspend
 */
@Fake
fun interface AsyncProducer<out T> {
    suspend fun produce(): T
}

/**
 * Scenario 48a: Covariant producer (explicit name for tests)
 */
@Fake
fun interface CovariantProducer<out T> {
    fun produce(): T
}

/**
 * Scenario 48b: Contravariant consumer (explicit name for tests)
 */
@Fake
fun interface ContravariantConsumer<in T> {
    fun consume(value: T)
}

/**
 * Scenario 48c: Invariant transformer (explicit name for tests)
 */
@Fake
fun interface InvariantTransformer<T> {
    fun transform(value: T): T
}

/**
 * Scenario 48d: Covariant list producer
 */
@Fake
fun interface CovariantListProducer<out T> {
    fun produce(): List<T>
}

/**
 * Scenario 48e: Contravariant list consumer
 */
@Fake
fun interface ContravariantListConsumer<in T> {
    fun consume(list: List<T>)
}

/**
 * Scenario 48f: Bivariant mapper (alias for VariantTransformer)
 */
@Fake
fun interface BivariantMapper<in T, out R> {
    fun map(input: T): R
}

// ============================================================================
// Phase 8: SAM Edge Cases (P3 - Edge Cases)
// ============================================================================

/**
 * Scenario 49: SAM with varargs
 */
@Fake
fun interface VarargsProcessor {
    fun process(vararg items: String): List<String>
}

/**
 * Scenario 50: SAM with star projection
 */
@Fake
fun interface StarProjectionHandler {
    fun handle(items: List<*>): Int
}

/**
 * Scenario 51: SAM with recursive generic
 */
@Fake
fun interface RecursiveComparator<T : Comparable<T>> {
    fun compare(a: T, b: T): Int
}

/**
 * Scenario 52: SAM with Array
 */
@Fake
fun interface ArrayProcessor<T> {
    fun process(items: Array<T>): Array<T>
}

/**
 * Scenario 53: SAM with primitive array
 */
@Fake
fun interface IntArrayProcessor {
    fun process(items: IntArray): IntArray
}

/**
 * Scenario 53a: Recursive generic (alias for RecursiveComparator)
 */
@Fake
fun interface RecursiveGeneric<T : Comparable<T>> {
    fun process(item: T): Int
}

/**
 * Scenario 53b: Nested generic mapper
 */
@Fake
fun interface NestedGenericMapper<T, R> {
    fun map(nested: List<List<T>>): List<List<R>>
}

/**
 * Scenario 53c: Complex bound handler with multiple constraints
 */
@Fake
fun interface ComplexBoundHandler<T> where T : CharSequence, T : Comparable<T> {
    fun handle(item: T): Int
}
