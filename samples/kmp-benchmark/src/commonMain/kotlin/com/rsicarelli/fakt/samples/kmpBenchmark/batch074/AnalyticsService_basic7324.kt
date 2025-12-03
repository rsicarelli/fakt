// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch074

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic7324 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic7324_1 : AnalyticsService_basic7324 {
    fun identify(userId: String)
}
