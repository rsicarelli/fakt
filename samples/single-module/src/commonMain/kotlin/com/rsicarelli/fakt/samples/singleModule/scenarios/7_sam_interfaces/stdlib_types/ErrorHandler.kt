// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.stdlibTypes

import com.rsicarelli.fakt.Fake

/**
 * Scenario 33: Result with Error handling
 */
@Fake
fun interface ErrorHandler<T> {
    fun handle(result: Result<T>): T?
}
