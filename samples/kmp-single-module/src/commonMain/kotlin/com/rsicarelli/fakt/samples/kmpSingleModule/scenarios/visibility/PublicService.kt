// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.visibility

import com.rsicarelli.fakt.Fake

/**
 * Public interface for visibility propagation testing.
 *
 * Tests that generated fakes correctly inherit public visibility when
 * compiled with explicitApi() mode enabled.
 */
@Fake
public interface PublicService {
    public fun doWork(): String

    public val status: Boolean
}
