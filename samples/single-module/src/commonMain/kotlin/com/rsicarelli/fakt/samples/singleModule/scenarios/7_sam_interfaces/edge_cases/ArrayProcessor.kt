// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases

import com.rsicarelli.fakt.Fake

/**
 * Scenario 52: SAM with Array
 */
@Fake
fun interface ArrayProcessor<T> {
    fun process(items: Array<T>): Array<T>
}
