// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch099

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic9834 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic9834_1 : AnalyticsService_basic9834 {
    fun identify(userId: String)
}
