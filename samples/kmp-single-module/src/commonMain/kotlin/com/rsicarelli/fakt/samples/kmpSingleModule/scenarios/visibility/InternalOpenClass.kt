// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.visibility

import com.rsicarelli.fakt.Fake

/**
 * Internal open class for visibility propagation testing.
 *
 * Tests that generated fakes correctly inherit internal visibility
 * for open class implementations.
 */
@Fake
internal open class InternalOpenClass {
    internal open fun process(): String = ""
}
