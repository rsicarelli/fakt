// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch057

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic5634 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic5634_1 : AnalyticsService_basic5634 {
    fun identify(userId: String)
}
