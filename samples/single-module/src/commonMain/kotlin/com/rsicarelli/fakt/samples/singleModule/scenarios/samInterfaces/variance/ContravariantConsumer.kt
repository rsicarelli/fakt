// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 48b: Contravariant consumer (explicit name for tests)
 */
@Fake
fun interface ContravariantConsumer<in T> {
    fun consume(value: T)
}
