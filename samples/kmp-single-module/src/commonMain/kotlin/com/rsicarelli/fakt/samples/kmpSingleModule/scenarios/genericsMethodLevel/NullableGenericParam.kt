// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsMethodLevel

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.MutabilityMode

/** Feature flag used to reproduce nullable generic parameters (#147). */
sealed class FeatureFlag(val key: String)

/** Boolean toggle flag. */
class ToggleFlag(key: String) : FeatureFlag(key)

/** Experiment flag, a subtype of [FeatureFlag]. */
class ExperimentFlag(key: String) : FeatureFlag(key)

/**
 * Regression: method-level generic used as a nullable parameter type (#147)
 *
 * **Pattern**: NullableMethodTypeParameterParam
 *
 * **Previously failing** (IR path paired overloads by name only, mixing their signatures):
 * - `OverrideRepo`'s call verifier declared `wasCalledWith(experiment: FeatureFlag, override: T?)`,
 *   combining the parameter names of one overload with the types of the other and referencing a
 *   type parameter that is not in scope of the verifier class.
 */
@Fake
interface OverrideStore {
    fun <T> setLocalOverride(feature: FeatureFlag, override: T? = null)
}

/** Same method on a mutable abstract class, overloaded as in the original report (#147). */
@Fake(mutability = MutabilityMode.MUTABLE)
abstract class OverrideRepo {
    abstract fun <T> setLocalOverride(feature: FeatureFlag, override: T? = null)

    abstract fun setLocalOverride(experiment: ExperimentFlag, override: String? = null)
}
