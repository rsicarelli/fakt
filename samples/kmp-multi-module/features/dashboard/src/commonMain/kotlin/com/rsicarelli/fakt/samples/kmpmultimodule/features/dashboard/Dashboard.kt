// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.dashboard

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger

// Dashboard feature - Analytics and metrics dashboard
data class DashboardData(val activeUsers: Int, val totalOrders: Int, val revenue: Double)

@Fake
interface DashboardUseCase {
    suspend fun loadDashboardData(analytics: Analytics, logger: Logger): DashboardData

    suspend fun trackDashboardView(analytics: Analytics)
}
