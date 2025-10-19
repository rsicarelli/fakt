// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.basic

import com.rsicarelli.fakt.Fake

/**
 * Scenario 2: SAM with nullable types
 */
@Fake
fun interface NullableHandler {
    fun handle(input: String?): String?
}
