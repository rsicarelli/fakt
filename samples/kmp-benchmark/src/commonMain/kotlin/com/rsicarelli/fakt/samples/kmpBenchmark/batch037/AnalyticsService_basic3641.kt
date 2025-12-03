// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch037

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic3641 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic3641_1 : AnalyticsService_basic3641 {
    fun identify(userId: String)
}
