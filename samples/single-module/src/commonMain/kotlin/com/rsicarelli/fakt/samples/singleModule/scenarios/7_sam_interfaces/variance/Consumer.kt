// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 44: Contravariant SAM (in)
 */
@Fake
fun interface Consumer<in T> {
    fun consume(item: T)
}
