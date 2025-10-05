// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.basic

import com.rsicarelli.fakt.Fake

/**
 * Scenario 1: Simple SAM with primitives
 */
@Fake
fun interface IntValidator {
    fun validate(value: Int): Boolean
}
