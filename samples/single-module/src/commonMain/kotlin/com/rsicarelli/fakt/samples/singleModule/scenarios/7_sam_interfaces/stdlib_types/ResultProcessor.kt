// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types

import com.rsicarelli.fakt.Fake

/**
 * Scenario 29: Result processor
 */
@Fake
fun interface ResultProcessor<T> {
    fun process(input: T): Result<T>
}
