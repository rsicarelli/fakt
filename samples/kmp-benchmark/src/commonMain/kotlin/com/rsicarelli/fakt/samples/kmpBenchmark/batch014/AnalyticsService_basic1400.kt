// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch014

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic1400 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic1400_1 : AnalyticsService_basic1400 {
    fun identify(userId: String)
}
