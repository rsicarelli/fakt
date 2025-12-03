// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch024

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic2332 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic2332_1 : AnalyticsService_basic2332 {
    fun identify(userId: String)
}
