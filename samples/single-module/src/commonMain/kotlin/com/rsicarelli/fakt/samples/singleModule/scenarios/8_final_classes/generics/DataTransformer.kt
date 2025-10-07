// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.generics

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: GenericTransformationClass
 *
 * **Pattern**: Open class with two type parameters representing input/output transformation
 * **Priority**: P1 (High - Common Mapper/Transformer Pattern)
 *
 * **What it tests**:
 * - Two independent type parameters with different semantic meanings
 * - In/Out transformation pattern (like Function<In, Out>)
 * - Abstract method behavior (transform has no super implementation)
 * - Open method with super defaults (transformBatch, canTransform)
 *
 * **Expected behavior**:
 * ```kotlin
 * class FakeDataTransformerImpl<In, Out> : DataTransformer<In, Out>() {
 *     private var transformBehavior: (In) -> Out = { _ -> error("Configure transform behavior") }
 *     private var transformBatchBehavior: (List<In>) -> List<Out> = { inputs -> super.transformBatch(inputs) }
 *     private var canTransformBehavior: (In) -> Boolean = { input -> super.canTransform(input) }
 *     // ...
 * }
 *
 * inline fun <In, Out> fakeDataTransformer(
 *     configure: FakeDataTransformerConfig<In, Out>.() -> Unit = {}
 * ): DataTransformer<In, Out>
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * // String to Int transformer
 * val stringToInt: DataTransformer<String, Int> = fakeDataTransformer {
 *     transform { input -> input.toIntOrNull() ?: 0 }
 * }
 *
 * // User to UserDto transformer
 * val userToDtoTransformer: DataTransformer<User, UserDto> = fakeDataTransformer {
 *     transform { user -> UserDto(user.id, user.name) }
 *     canTransform { user -> user.isValid }
 * }
 * ```
 */
@Fake
open class DataTransformer<In, Out> {
    /**
     * Transforms a single input value to output value.
     * Note: This is effectively abstract (no meaningful super implementation).
     */
    open fun transform(input: In): Out {
        error("Not implemented")
    }

    /**
     * Transforms a batch of input values.
     * Default implementation uses transform() for each input.
     */
    open fun transformBatch(inputs: List<In>): List<Out> = emptyList()

    /**
     * Checks if the input can be transformed.
     */
    open fun canTransform(input: In): Boolean = false
}
