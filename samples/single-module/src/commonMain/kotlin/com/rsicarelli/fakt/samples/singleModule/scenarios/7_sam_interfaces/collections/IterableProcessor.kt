// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 28: Iterable processor
 */
@Fake
fun interface IterableProcessor<T> {
    fun process(items: Iterable<T>): Iterable<T>
}
