// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 28c: Array handler
 */
@Fake
fun interface ArrayHandler<T> {
    fun handle(items: Array<T>): Array<T>
}
