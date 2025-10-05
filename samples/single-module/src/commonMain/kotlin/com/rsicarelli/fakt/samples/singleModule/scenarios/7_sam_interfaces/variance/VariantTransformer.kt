// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 45: Mixed variance
 */
@Fake
fun interface VariantTransformer<in T, out R> {
    fun transform(input: T): R
}
