// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch079

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic7836 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic7836_1 : AnalyticsService_basic7836 {
    fun identify(userId: String)
}
