// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic

import com.rsicarelli.fakt.Fake

/**
 * Scenario 1: Simple SAM with primitives
 */
@Fake
fun interface IntValidator {
    fun validate(value: Int): Boolean
}
