// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch059

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic5883 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic5883_1 : AnalyticsService_basic5883 {
    fun identify(userId: String)
}
