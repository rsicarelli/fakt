// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 28b: Map processor with three type params
 */
@Fake
fun interface MapProcessor<K, V, R> {
    fun process(map: Map<K, V>): Map<K, R>
}
