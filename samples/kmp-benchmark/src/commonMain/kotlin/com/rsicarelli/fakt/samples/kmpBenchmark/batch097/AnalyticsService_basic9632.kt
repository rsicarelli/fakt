// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch097

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic9632 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic9632_1 : AnalyticsService_basic9632 {
    fun identify(userId: String)
}
