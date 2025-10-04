// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * Advanced feature test: @Fake(trackCalls = true).
 *
 * Tests advanced annotation parameter support for future features.
 * The trackCalls parameter would enable call tracking/verification in generated fakes.
 * Currently demonstrates annotation parameter parsing (feature not yet implemented).
 */
@Fake(trackCalls = true)
interface AnalyticsService {
    fun track(event: String)
}
