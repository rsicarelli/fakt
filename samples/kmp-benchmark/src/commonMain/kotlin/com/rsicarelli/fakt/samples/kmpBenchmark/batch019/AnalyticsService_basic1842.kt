// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch019

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic1842 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic1842_1 : AnalyticsService_basic1842 {
    fun identify(userId: String)
}
