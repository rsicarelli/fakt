// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch096

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic9517 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic9517_1 : AnalyticsService_basic9517 {
    fun identify(userId: String)
}
