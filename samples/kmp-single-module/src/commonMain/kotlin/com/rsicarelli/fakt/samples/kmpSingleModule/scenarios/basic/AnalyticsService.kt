// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.basic

import com.rsicarelli.fakt.Fake

/**
 * Advanced analytics service interface.
 *
 * Demonstrates fake generation for services with simple tracking methods.
 */
@Fake
interface AnalyticsService {
    fun track(event: String)
}

@Fake
interface AnalyticsServiceExtended : AnalyticsService {
    fun identify(userId: String)
}
