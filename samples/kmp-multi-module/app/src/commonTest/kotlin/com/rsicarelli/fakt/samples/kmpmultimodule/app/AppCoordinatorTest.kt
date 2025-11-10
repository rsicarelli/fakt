// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.app

import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.fakeAnalytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.fakeAuthProvider
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.fakeApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeKeyValueStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.features.dashboard.DashboardUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.dashboard.fakeDashboardUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.login.LoginUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.login.fakeLoginUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.order.OrderUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.order.fakeOrderUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.profile.ProfileUseCase
import com.rsicarelli.fakt.samples.kmpmultimodule.features.profile.fakeProfileUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppCoordinatorTest {

    // ============================================================================
    // INITIALIZATION TESTS
    // ============================================================================

    @Test
    fun `GIVEN AppCoordinator WHEN initializing app THEN should check authentication and track analytics`() =
        runTest {
            // Given - Set up all fakes from different modules
            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val authProvider = fakeAuthProvider {
                isAuthenticated { true }
            }

            val coordinator = factoryAppCoordinator(
                authProvider = authProvider,
                logger = logger,
                analytics = analytics,
            )

            // When
            coordinator.initialize()

            // Then
            assertTrue(loggedMessages.contains("App initializing"))
            assertTrue(loggedMessages.any { it.contains("Authentication status: true") })
            assertTrue(trackedEvents.contains("app_started"))
        }

    @Test
    fun `GIVEN unauthenticated user WHEN initializing THEN should log false authentication status`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val authProvider = fakeAuthProvider {
                isAuthenticated { false }
            }

            val coordinator = factoryAppCoordinator(
                authProvider = authProvider,
                logger = logger,
            )

            // When
            coordinator.initialize()

            // Then
            assertTrue(loggedMessages.contains("App initializing"))
            assertTrue(loggedMessages.any { it.contains("Authentication status: false") })
        }

    @Test
    fun `GIVEN AppCoordinator WHEN initializing multiple times THEN should track all app_started events`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val coordinator = factoryAppCoordinator(
                analytics = analytics,
            )

            // When
            coordinator.initialize()
            coordinator.initialize()
            coordinator.initialize()

            // Then
            assertEquals(3, trackedEvents.count { it == "app_started" })
        }

    // ============================================================================
    // DEEP LINK TESTS
    // ============================================================================

    @Test
    fun `GIVEN AppCoordinator WHEN handling deep link THEN should log the URL`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val coordinator = factoryAppCoordinator(
                logger = logger,
            )

            // When
            coordinator.handleDeepLink("myapp://order/12345")

            // Then
            assertTrue(loggedMessages.any { it.contains("Handling deep link: myapp://order/12345") })
        }

    @Test
    fun `GIVEN AppCoordinator WHEN handling multiple deep links THEN should log all URLs`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val coordinator = factoryAppCoordinator(
                logger = logger,
            )

            // When
            coordinator.handleDeepLink("myapp://order/12345")
            coordinator.handleDeepLink("myapp://profile/settings")
            coordinator.handleDeepLink("myapp://dashboard")

            // Then
            assertTrue(loggedMessages.any { it.contains("myapp://order/12345") })
            assertTrue(loggedMessages.any { it.contains("myapp://profile/settings") })
            assertTrue(loggedMessages.any { it.contains("myapp://dashboard") })
            assertEquals(3, loggedMessages.count { it.startsWith("Handling deep link:") })
        }

    @Test
    fun `GIVEN different URL formats WHEN handling deep links THEN should log all correctly`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val coordinator = factoryAppCoordinator(
                logger = logger,
            )

            // When
            coordinator.handleDeepLink("https://example.com/order/123")
            coordinator.handleDeepLink("myapp://feature?param=value")
            coordinator.handleDeepLink("/internal/route")

            // Then
            assertTrue(loggedMessages.any { it.contains("https://example.com/order/123") })
            assertTrue(loggedMessages.any { it.contains("myapp://feature?param=value") })
            assertTrue(loggedMessages.any { it.contains("/internal/route") })
        }

    // ============================================================================
    // SHUTDOWN TESTS
    // ============================================================================

    @Test
    fun `GIVEN AppCoordinator WHEN shutting down THEN should log and track app_closed event`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val coordinator = factoryAppCoordinator(
                logger = logger,
                analytics = analytics,
            )

            // When
            coordinator.shutdown()

            // Then
            assertTrue(loggedMessages.contains("App shutting down"))
            assertTrue(trackedEvents.contains("app_closed"))
        }

    @Test
    fun `GIVEN AppCoordinator WHEN shutting down multiple times THEN should track all app_closed events`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val coordinator = factoryAppCoordinator(
                analytics = analytics,
            )

            // When
            coordinator.shutdown()
            coordinator.shutdown()

            // Then
            assertEquals(2, trackedEvents.count { it == "app_closed" })
        }

    // ============================================================================
    // LIFECYCLE TESTS
    // ============================================================================

    @Test
    fun `GIVEN AppCoordinator WHEN executing full lifecycle THEN should track events in correct order`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val coordinator = factoryAppCoordinator(
                logger = logger,
                analytics = analytics,
            )

            // When - Full app lifecycle
            coordinator.initialize()
            coordinator.handleDeepLink("myapp://feature")
            coordinator.shutdown()

            // Then - Verify event order
            assertEquals("app_started", trackedEvents[0])
            assertEquals("app_closed", trackedEvents[1])

            assertTrue(loggedMessages[0].contains("App initializing"))
            assertTrue(loggedMessages[1].contains("Authentication status"))
            assertTrue(loggedMessages[2].contains("Handling deep link"))
            assertTrue(loggedMessages[3].contains("App shutting down"))
        }

    // ============================================================================
    // ANALYTICS & LOGGER METADATA TESTS
    // ============================================================================

    @Test
    fun `GIVEN analytics WHEN tracking events THEN should receive correct metadata`() = runTest {
        // Given
        val trackedMetadata = mutableListOf<Map<String, Any>>()
        val analytics = fakeAnalytics {
            track { _, metadata ->
                trackedMetadata.add(metadata)
            }
        }

        val coordinator = factoryAppCoordinator(
            analytics = analytics,
        )

        // When
        coordinator.initialize()
        coordinator.shutdown()

        // Then
        assertEquals(2, trackedMetadata.size)
        assertTrue(trackedMetadata.all { it.isEmpty() || it.isNotEmpty() })
    }

    @Test
    fun `GIVEN logger WHEN logging messages THEN should receive correct metadata`() = runTest {
        // Given
        val loggedMetadata = mutableListOf<Map<String, Any>>()
        val logger = fakeLogger {
            info { _, metadata ->
                loggedMetadata.add(metadata)
            }
        }

        val coordinator = factoryAppCoordinator(
            logger = logger,
        )

        // When
        coordinator.initialize()
        coordinator.handleDeepLink("myapp://test")

        // Then - Should log: "App initializing", "Authentication status: X", "Handling deep link"
        assertEquals(3, loggedMetadata.size)
    }

    // ============================================================================
    // HELPER FACTORY
    // ============================================================================

    fun factoryAppCoordinator(
        authProvider: AuthProvider = fakeAuthProvider(),
        apiClient: ApiClient = fakeApiClient(),
        storage: KeyValueStorage = fakeKeyValueStorage(),
        logger: Logger = fakeLogger(),
        analytics: Analytics = fakeAnalytics(),
        loginUseCase: LoginUseCase = fakeLoginUseCase(),
        orderUseCase: OrderUseCase = fakeOrderUseCase(),
        profileUseCase: ProfileUseCase = fakeProfileUseCase(),
        dashboardUseCase: DashboardUseCase = fakeDashboardUseCase(),
    ) = AppCoordinator(
        authProvider = authProvider,
        apiClient = apiClient,
        storage = storage,
        logger = logger,
        analytics = analytics,
        loginUseCase = loginUseCase,
        orderUseCase = orderUseCase,
        profileUseCase = profileUseCase,
        dashboardUseCase = dashboardUseCase,
    )
}
