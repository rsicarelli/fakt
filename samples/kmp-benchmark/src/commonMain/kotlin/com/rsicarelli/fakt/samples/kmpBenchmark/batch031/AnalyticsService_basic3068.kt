// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch031

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic3068 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic3068_1 : AnalyticsService_basic3068 {
    fun identify(userId: String)
}
