// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 28c: Array handler
 */
@Fake
fun interface ArrayHandler<T> {
    fun handle(items: Array<T>): Array<T>
}
