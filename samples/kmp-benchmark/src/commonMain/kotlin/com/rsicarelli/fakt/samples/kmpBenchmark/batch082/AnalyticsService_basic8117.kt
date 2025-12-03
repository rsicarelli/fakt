// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch082

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic8117 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic8117_1 : AnalyticsService_basic8117 {
    fun identify(userId: String)
}
