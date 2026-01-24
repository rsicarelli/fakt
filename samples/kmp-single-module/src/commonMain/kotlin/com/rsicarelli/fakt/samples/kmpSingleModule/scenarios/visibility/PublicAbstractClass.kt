// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.visibility

import com.rsicarelli.fakt.Fake

/**
 * Public abstract class for visibility propagation testing.
 *
 * Tests that generated fakes correctly inherit public visibility
 * for abstract class implementations.
 */
@Fake
public abstract class PublicAbstractClass {
    public abstract fun compute(): Int
}
