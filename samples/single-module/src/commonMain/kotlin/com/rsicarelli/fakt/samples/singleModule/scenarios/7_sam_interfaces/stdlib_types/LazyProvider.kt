// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types

import com.rsicarelli.fakt.Fake

/**
 * Scenario 34: Lazy value provider
 */
@Fake
fun interface LazyProvider<T> {
    fun provide(): Lazy<T>
}
