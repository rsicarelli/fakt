// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch075

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic7415 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic7415_1 : AnalyticsService_basic7415 {
    fun identify(userId: String)
}
