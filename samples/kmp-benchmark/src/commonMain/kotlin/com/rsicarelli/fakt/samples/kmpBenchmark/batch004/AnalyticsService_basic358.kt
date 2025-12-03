// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch004

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic358 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic358_1 : AnalyticsService_basic358 {
    fun identify(userId: String)
}
