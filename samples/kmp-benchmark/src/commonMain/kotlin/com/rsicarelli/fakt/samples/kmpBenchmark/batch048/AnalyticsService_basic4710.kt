// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch048

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic4710 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic4710_1 : AnalyticsService_basic4710 {
    fun identify(userId: String)
}
