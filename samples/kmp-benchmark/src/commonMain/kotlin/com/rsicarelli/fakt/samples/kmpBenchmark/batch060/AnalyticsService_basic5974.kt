// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch060

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic5974 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic5974_1 : AnalyticsService_basic5974 {
    fun identify(userId: String)
}
