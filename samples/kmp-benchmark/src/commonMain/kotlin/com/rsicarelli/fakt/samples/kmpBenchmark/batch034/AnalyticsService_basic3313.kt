// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch034

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic3313 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic3313_1 : AnalyticsService_basic3313 {
    fun identify(userId: String)
}
