// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch064

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic6351 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic6351_1 : AnalyticsService_basic6351 {
    fun identify(userId: String)
}
