// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch085

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic8465 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic8465_1 : AnalyticsService_basic8465 {
    fun identify(userId: String)
}
