// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch012

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic1163 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic1163_1 : AnalyticsService_basic1163 {
    fun identify(userId: String)
}
