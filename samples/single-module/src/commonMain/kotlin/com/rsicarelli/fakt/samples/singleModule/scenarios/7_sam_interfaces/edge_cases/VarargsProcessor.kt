// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases

import com.rsicarelli.fakt.Fake

/**
 * Scenario 49: SAM with varargs
 */
@Fake
fun interface VarargsProcessor {
    fun process(vararg items: String): List<String>
}
