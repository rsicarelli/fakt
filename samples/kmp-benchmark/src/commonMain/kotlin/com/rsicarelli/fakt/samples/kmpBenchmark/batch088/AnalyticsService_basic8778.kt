// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch088

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic8778 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic8778_1 : AnalyticsService_basic8778 {
    fun identify(userId: String)
}
