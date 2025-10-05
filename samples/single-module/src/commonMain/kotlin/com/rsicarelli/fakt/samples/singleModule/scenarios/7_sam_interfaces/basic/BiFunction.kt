// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.basic

import com.rsicarelli.fakt.Fake

/**
 * Scenario 5: SAM with multiple parameters
 */
@Fake
fun interface BiFunction {
    fun apply(a: Int, b: Int): Int
}
