// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsBasic

import com.rsicarelli.fakt.Fake

/**
 * P1.3: Generic interface with lambdas and method-level generics.
 *
 * Tests combination of:
 * - Class-level generic (T)
 * - Lambda parameters with generic types ((T) -> String)
 * - Method-level type parameters (<R>)
 * - Generic transformations (List<T> -> List<R>)
 * Common in event processing pipelines with type-safe transformations.
 */
@Fake
interface GenericEventProcessor<T> {
    fun process(
        item: T,
        processor: (T) -> String,
    ): String

    fun <R> transform(
        items: List<T>,
        transformer: (T) -> R,
    ): List<R>
}
