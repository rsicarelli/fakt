// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch043

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic4274 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic4274_1 : AnalyticsService_basic4274 {
    fun identify(userId: String)
}
