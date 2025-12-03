// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch066

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic6507 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic6507_1 : AnalyticsService_basic6507 {
    fun identify(userId: String)
}
