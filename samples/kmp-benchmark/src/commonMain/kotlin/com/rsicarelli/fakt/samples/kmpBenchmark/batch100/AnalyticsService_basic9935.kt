// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch100

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic9935 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic9935_1 : AnalyticsService_basic9935 {
    fun identify(userId: String)
}
