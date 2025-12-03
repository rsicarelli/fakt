// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch025

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic2434 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic2434_1 : AnalyticsService_basic2434 {
    fun identify(userId: String)
}
