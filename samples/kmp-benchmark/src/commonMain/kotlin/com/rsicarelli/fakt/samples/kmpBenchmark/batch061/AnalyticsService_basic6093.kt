// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch061

import com.rsicarelli.fakt.Fake

@Fake
interface AnalyticsService_basic6093 {
    fun track(event: String)
}

@Fake
interface AnalyticsService_basic6093_1 : AnalyticsService_basic6093 {
    fun identify(userId: String)
}
