// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 48f: Bivariant mapper (alias for VariantTransformer)
 */
@Fake
fun interface BivariantMapper<in T, out R> {
    fun map(input: T): R
}
