// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics

import com.rsicarelli.fakt.Fake

/**
 * Scenario 13: Generic SAM with Result
 */
@Fake
fun interface ResultHandler<T> {
    fun handle(input: T): Result<T>
}
