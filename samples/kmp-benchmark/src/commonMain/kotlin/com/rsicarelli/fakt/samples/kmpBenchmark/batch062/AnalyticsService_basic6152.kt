// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch062

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic6152 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic6152_1 : AnalyticsService_basic6152 {
    fun identify(userId: String)
}
