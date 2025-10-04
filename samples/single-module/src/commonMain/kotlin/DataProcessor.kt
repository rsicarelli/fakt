// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * Phase 3 test interface: Method-level generics only (no class-level generics).
 *
 * Tests that method-level type parameters are preserved:
 * - fun <T> process(item: T): T
 * - fun <R> transform(input: String): R
 */
@Fake
interface DataProcessor {
    fun <T> process(item: T): T

    fun <R> transform(input: String): R

    fun getData(): String
}

/**
 * Phase 3 test interface: MIXED GENERICS (class-level + method-level).
 *
 * Tests that both class-level and method-level type parameters work together:
 * - Class-level T: shared across all methods
 * - Method-level R: specific to each method
 *
 * Expected generation:
 * ```kotlin
 * class FakeMixedProcessor<T> : MixedProcessor<T> {
 *     override fun process(item: T): T { ... }
 *     override fun <R> transform(item: T): R { ... }
 * }
 *
 * inline fun <reified T> fakeMixedProcessor(
 *     configure: FakeMixedProcessorConfig<T>.() -> Unit = {}
 * ): MixedProcessor<T>
 * ```
 */
@Fake
interface MixedProcessor<T> {
    /** Uses class-level T */
    fun process(item: T): T

    /** Uses both class-level T and method-level R */
    fun <R> transform(item: T): R

    /** Non-generic method */
    fun reset()
}
