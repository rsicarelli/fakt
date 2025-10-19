// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.variance

import com.rsicarelli.fakt.Fake

/**
 * Scenario 44: Contravariant SAM (in)
 */
@Fake
fun interface Consumer<in T> {
    fun consume(item: T)
}
