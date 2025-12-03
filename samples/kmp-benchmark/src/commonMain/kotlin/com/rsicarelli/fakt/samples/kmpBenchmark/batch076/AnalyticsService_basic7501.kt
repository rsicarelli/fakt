// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch076

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic7501 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic7501_1 : AnalyticsService_basic7501 {
    fun identify(userId: String)
}
