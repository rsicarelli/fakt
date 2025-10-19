// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.dashboard

import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Dashboard screen state.
 */
sealed class DashboardState {
    data object Idle : DashboardState()
    data object Loading : DashboardState()
    data class Success(val data: DashboardData) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

/**
 * Vanilla ViewModel for Dashboard feature (no Android dependencies).
 *
 * Demonstrates production-ready patterns:
 * - StateFlow for reactive state management
 * - K2.2+ backing fields pattern (get() = _field)
 * - Coroutine scope for async operations
 * - Clean separation of concerns
 *
 * This serves as a real-world example for testing with Fakt + Turbine.
 *
 * NOTE: Call counts are automatically tracked by Fakt fakes!
 * No need to manually track counters - use `useCase.methodNameCallCount` in tests.
 */
class DashboardViewModel(
    private val useCase: DashboardUseCase,
    private val analytics: Analytics,
    private val logger: Logger,
    private val scope: CoroutineScope,
) {
    // State - K2.2+ backing field pattern
    private val _state = MutableStateFlow<DashboardState>(DashboardState.Idle)
    val state: StateFlow<DashboardState>
        get() = _state

    /**
     * Load dashboard data.
     * Transitions: Idle/Success -> Loading -> Success/Error
     */
    fun loadDashboard() {
        scope.launch {
            try {
                _state.update { DashboardState.Loading }

                logger.info("Loading dashboard data")
                val data = useCase.loadDashboardData(analytics, logger)

                _state.update { DashboardState.Success(data) }
                logger.info("Dashboard loaded successfully: $data")

                analytics.track("dashboard_loaded", mapOf("success" to "true"))
            } catch (e: Exception) {
                _state.update { DashboardState.Error(e.message ?: "Unknown error") }
                logger.error("Failed to load dashboard", e, mapOf("error" to e.message.orEmpty()))

                analytics.track("dashboard_load_failed", mapOf("error" to e.message.orEmpty()))
            }
        }
    }

    /**
     * Refresh dashboard data.
     * Only works if current state is Success.
     */
    fun refresh() {
        scope.launch {
            // Only refresh if we have existing data (thread-safe check)
            val currentState = _state.value
            if (currentState is DashboardState.Success) {
                try {
                    _state.update { DashboardState.Loading }

                    logger.info("Refreshing dashboard data")
                    val data = useCase.loadDashboardData(analytics, logger)

                    _state.update { DashboardState.Success(data) }
                    logger.info("Dashboard refreshed successfully")

                    analytics.track("dashboard_refreshed", mapOf("success" to "true"))
                } catch (e: Exception) {
                    _state.update { DashboardState.Error(e.message ?: "Unknown error") }
                    logger.error("Failed to refresh dashboard", e, mapOf("error" to e.message.orEmpty()))

                    analytics.track("dashboard_refresh_failed", mapOf("error" to e.message.orEmpty()))
                }
            }
        }
    }
}
