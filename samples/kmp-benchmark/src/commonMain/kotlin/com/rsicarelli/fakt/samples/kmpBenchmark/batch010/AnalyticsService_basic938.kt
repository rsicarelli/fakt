// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch010

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic938 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic938_1 : AnalyticsService_basic938 {
    fun identify(userId: String)
}
