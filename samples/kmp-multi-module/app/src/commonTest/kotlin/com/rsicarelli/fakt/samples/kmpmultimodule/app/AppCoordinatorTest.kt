// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.app

import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.fakeAnalytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.fakeAuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.fakeApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeKeyValueStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.features.dashboard.fakeDashboardUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.login.fakeLoginUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.order.fakeOrderUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.profile.fakeProfileUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for AppCoordinator demonstrating full-stack cross-module integration.
 *
 * This test shows how fakes from multiple modules (core + features) work together.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppCoordinatorTest {

    @Test
    fun `GIVEN AppCoordinator WHEN initializing app THEN should check authentication and track analytics`() =
        runTest {
            // Given - Set up all fakes from different modules
            var trackedEvents = mutableListOf<String>()
            val analytics =
                fakeAnalytics {
                    track { eventName, _ ->
                        trackedEvents.add(eventName)
                    }
                }

            var loggedMessages = mutableListOf<String>()
            val logger =
                fakeLogger {
                    info { message, _ ->
                        loggedMessages.add(message)
                    }
                }

            val authProvider =
                fakeAuthProvider {
                    isAuthenticated { true }
                }

            val apiClient = fakeApiClient {}
            val storage = fakeKeyValueStorage {}

            // Feature use cases
            val loginUseCase = fakeLoginUseCase {}
            val orderUseCase = fakeOrderUseCase {}
            val profileUseCase = fakeProfileUseCase {}
            val dashboardUseCase = fakeDashboardUseCase {}

            val coordinator =
                AppCoordinator(
                    authProvider,
                    apiClient,
                    storage,
                    logger,
                    analytics,
                    loginUseCase,
                    orderUseCase,
                    profileUseCase,
                    dashboardUseCase,
                )

            // When
            coordinator.initialize()

            // Then
            assertTrue(loggedMessages.contains("App initializing"))
            assertTrue(trackedEvents.contains("app_started"))
        }

    @Test
    fun `GIVEN AppCoordinator WHEN handling deep link THEN should log the URL`() =
        runTest {
            // Given
            var loggedMessages = mutableListOf<String>()
            val logger =
                fakeLogger {
                    info { message, _ ->
                        loggedMessages.add(message)
                    }
                }

            val coordinator =
                AppCoordinator(
                    fakeAuthProvider {},
                    fakeApiClient {},
                    fakeKeyValueStorage {},
                    logger,
                    fakeAnalytics {},
                    fakeLoginUseCase {},
                    fakeOrderUseCase {},
                    fakeProfileUseCase {},
                    fakeDashboardUseCase {},
                )

            // When
            coordinator.handleDeepLink("myapp://order/12345")

            // Then
            assertTrue(loggedMessages.any { it.contains("myapp://order/12345") })
        }
}
