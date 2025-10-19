// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.app

import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.features.dashboard.DashboardUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.login.LoginUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.order.OrderUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.profile.ProfileUseCase

// ============================================================================
// APP MODULE - Application Coordinator
// Lightweight module that integrates all features and coordinates app flow
// Dependencies: ALL features + ALL core modules (transitive)
// ============================================================================

/**
 * Main application coordinator.
 * In a real app, this would handle navigation, app lifecycle, and feature coordination.
 *
 * This is intentionally lightweight - it just orchestrates features, doesn't contain business logic.
 */
class AppCoordinator(
    // Core infrastructure
    private val authProvider: AuthProvider,
    private val apiClient: ApiClient,
    private val storage: KeyValueStorage,
    private val logger: Logger,
    private val analytics: Analytics,
    // Feature use cases
    private val loginUseCase: LoginUseCase,
    private val orderUseCase: OrderUseCase,
    private val profileUseCase: ProfileUseCase,
    private val dashboardUseCase: DashboardUseCase,
) {
    suspend fun initialize() {
        logger.info("App initializing")
        analytics.track("app_started")

        // Check if user is authenticated
        val isAuthenticated = authProvider.isAuthenticated()
        logger.info("Authentication status: $isAuthenticated")
    }

    suspend fun handleDeepLink(url: String) {
        logger.info("Handling deep link: $url")
        // Deep link routing logic would go here
    }

    fun shutdown() {
        logger.info("App shutting down")
        analytics.track("app_closed")
    }
}

/**
 * Application configuration.
 */
data class AppConfig(
    val apiBaseUrl: String,
    val environment: String,
    val loggingEnabled: Boolean,
    val analyticsEnabled: Boolean,
)
