// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * Scenario 53: SAM with primitive array
 */
@Fake
fun interface IntArrayProcessor {
    fun process(items: IntArray): IntArray
}
