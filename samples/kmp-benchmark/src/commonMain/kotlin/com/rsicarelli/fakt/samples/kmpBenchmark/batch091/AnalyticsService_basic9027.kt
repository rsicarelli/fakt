// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch091

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic9027 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic9027_1 : AnalyticsService_basic9027 {
    fun identify(userId: String)
}
