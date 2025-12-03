// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch094

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic9372 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic9372_1 : AnalyticsService_basic9372 {
    fun identify(userId: String)
}
