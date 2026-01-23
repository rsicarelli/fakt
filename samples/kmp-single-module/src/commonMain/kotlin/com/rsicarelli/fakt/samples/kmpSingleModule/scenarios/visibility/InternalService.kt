// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.visibility

import com.rsicarelli.fakt.Fake

/**
 * Internal interface for visibility propagation testing.
 *
 * Tests that generated fakes correctly inherit internal visibility when
 * compiled with explicitApi() mode enabled.
 */
@Fake
internal interface InternalService {
    fun doWork(): String

    val status: Boolean
}
