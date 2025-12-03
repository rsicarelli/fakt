// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch073

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic7231 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic7231_1 : AnalyticsService_basic7231 {
    fun identify(userId: String)
}
