// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch098

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic9732 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic9732_1 : AnalyticsService_basic9732 {
    fun identify(userId: String)
}
