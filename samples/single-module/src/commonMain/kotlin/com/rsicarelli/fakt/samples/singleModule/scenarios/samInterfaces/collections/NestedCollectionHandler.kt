// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 24: Nested collections
 */
@Fake
fun interface NestedCollectionHandler {
    fun handle(data: List<Map<String, Set<Int>>>): Map<String, List<Int>>
}
