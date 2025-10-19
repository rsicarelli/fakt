// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * Scenario 53b: Nested generic mapper
 */
@Fake
fun interface NestedGenericMapper<T, R> {
    fun map(nested: List<List<T>>): List<List<R>>
}
