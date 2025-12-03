// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch063

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic6226 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic6226_1 : AnalyticsService_basic6226 {
    fun identify(userId: String)
}
