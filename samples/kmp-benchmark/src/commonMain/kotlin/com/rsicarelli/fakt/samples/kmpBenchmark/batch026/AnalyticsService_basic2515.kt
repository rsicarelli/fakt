// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch026

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic2515 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic2515_1 : AnalyticsService_basic2515 {
    fun identify(userId: String)
}
