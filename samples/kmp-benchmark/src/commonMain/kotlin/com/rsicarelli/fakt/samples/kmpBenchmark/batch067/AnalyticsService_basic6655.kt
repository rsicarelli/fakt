// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch067

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic6655 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic6655_1 : AnalyticsService_basic6655 {
    fun identify(userId: String)
}
