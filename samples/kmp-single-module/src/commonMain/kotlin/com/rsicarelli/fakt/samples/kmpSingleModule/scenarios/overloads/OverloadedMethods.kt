// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.overloads

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.MutabilityMode

/** Feature flag hierarchy used to reproduce overloads over a sealed type (#146). */
sealed class Feature(val key: String)

/** Boolean feature toggle. */
class Toggle(key: String) : Feature(key)

/** Sample A/B test variant. */
enum class Variant {
    CONTROL,
    TREATMENT,
}

/** A/B test feature, a subtype of [Feature]. */
class ABTest<T : Enum<T>>(key: String) : Feature(key)

/**
 * Regression: overloaded methods (same name, different parameters) (#146)
 *
 * **Pattern**: MethodOverloads
 *
 * **Previously failing**:
 * - Both overloads generated `setLocalOverrideBehavior`, `setLocalOverrideCalls`,
 *   `OverloadedServiceSetLocalOverrideCall`, `verifySetLocalOverride` and a `setLocalOverride {}` DSL
 *   function, so the fake failed with conflicting declarations.
 */
@Fake
interface OverloadedService {
    fun <T> setLocalOverride(feature: Feature, override: T? = null)

    fun setLocalOverride(abTest: ABTest<*>, override: String? = null)

    fun find(id: Int): String

    fun find(name: String): String

    fun find(): List<String>

    suspend fun load(id: Int): String

    suspend fun load(ids: List<Int>): List<String>

    fun unique(): Int
}

/** Same scenario on an abstract class with mutable behaviors (#146). */
@Fake(mutability = MutabilityMode.MUTABLE)
abstract class OverloadedRepo {
    abstract fun <T> setLocalOverride(feature: Feature, override: T? = null)

    abstract fun setLocalOverride(abTest: ABTest<*>, override: String? = null)

    open fun count(): Int = 0

    open fun count(prefix: String): Int = prefix.length
}
