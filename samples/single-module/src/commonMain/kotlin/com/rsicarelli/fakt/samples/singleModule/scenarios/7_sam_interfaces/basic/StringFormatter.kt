// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.basic

import com.rsicarelli.fakt.Fake

/**
 * Scenario 6: SAM with String return
 */
@Fake
fun interface StringFormatter {
    fun format(value: Any): String
}
