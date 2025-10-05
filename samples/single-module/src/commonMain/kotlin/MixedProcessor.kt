// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * P2.1: Mixed generics test interface (class-level + method-level).
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
    fun process(item: T): T

    fun <R> transform(item: T): R

    fun reset()
}
