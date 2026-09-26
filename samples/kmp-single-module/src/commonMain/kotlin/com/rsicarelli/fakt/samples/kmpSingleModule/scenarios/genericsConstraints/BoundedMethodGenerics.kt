// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsConstraints

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.MutabilityMode

/** A/B test descriptor whose type parameter is bounded by a recursive enum constraint. */
data class ABTest<T : Enum<T>>(val key: String, val default: T)

/** Container whose type parameter is bounded by `Any` (non-nullable). */
data class NonNullBox<T : Any>(val value: T)

/** Sample enum used as an A/B test variant. */
enum class Variant {
    CONTROL,
    TREATMENT,
}

/**
 * Regression: method-level generics with upper bounds used as type arguments (#145)
 *
 * **Pattern**: BoundedMethodTypeParameterAsTypeArgument
 *
 * **Previously failing**:
 * - `fun <T : Enum<T>> abTestAsEnum(abTest: ABTest<T>): T` erased `ABTest<T>` to `ABTest<Any?>`
 *   in behaviors and call-history data classes, violating the `T : Enum<T>` bound of `ABTest`.
 * - The same happened for `T : Any` bounds (`NonNullBox<Any?>`).
 */
@Fake
interface ExperimentsService {
    fun <T : Enum<T>> abTestAsEnum(abTest: ABTest<T>): T

    fun <T : Any> unbox(box: NonNullBox<T>): T

    fun <T : Enum<T>> allVariants(tests: List<ABTest<T>>): Map<String, T>
}

/** Same scenario as [ExperimentsService] on an abstract class with mutable behaviors (#145). */
@Fake(mutability = MutabilityMode.MUTABLE)
abstract class ExperimentsRepo {
    abstract fun <T : Enum<T>> abTestAsEnum(abTest: ABTest<T>): T

    abstract fun <T : Any> unbox(box: NonNullBox<T>): T

    open fun <T : Enum<T>> isEnabled(abTest: ABTest<T>): Boolean = false
}
