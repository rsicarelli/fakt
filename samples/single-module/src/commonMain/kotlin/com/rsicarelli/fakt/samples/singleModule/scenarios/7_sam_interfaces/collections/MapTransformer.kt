// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 22: Map transformer
 */
@Fake
fun interface MapTransformer<K, V> {
    fun transform(map: Map<K, V>): Map<K, String>
}
