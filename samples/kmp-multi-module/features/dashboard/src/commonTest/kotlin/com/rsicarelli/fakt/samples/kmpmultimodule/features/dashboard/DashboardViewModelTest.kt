// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.rsicarelli.fakt.samples.kmpmultimodule.features.dashboard

import app.cash.turbine.test
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.fakeAnalytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardViewModelTest {

    // ============================================================================
    // STATE TRANSITION TESTS
    // ============================================================================

    @Test
    fun `GIVEN DashboardViewModel WHEN loading dashboard THEN should transition from Idle to Loading to Success`() =
        runTest {
            // Given
            val testData = DashboardData(activeUsers = 100, totalOrders = 200, revenue = 300.0)
            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ -> testData }
            }

            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When
            viewModel.state.test {
                // Initial state
                assertEquals(DashboardState.Idle, awaitItem())

                // Load
                viewModel.loadDashboard()
                runCurrent()

                // Should transition: Loading -> Success
                assertEquals(DashboardState.Loading, awaitItem())
                assertEquals(DashboardState.Success(testData), awaitItem())
            }
        }

    @Test
    fun `GIVEN DashboardViewModel WHEN loading fails THEN should transition to Error state`() =
        runTest {
            // Given
            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ ->
                    throw RuntimeException("Network error")
                }
            }

            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When
            viewModel.state.test {
                assertEquals(DashboardState.Idle, awaitItem())

                viewModel.loadDashboard()
                runCurrent()

                assertEquals(DashboardState.Loading, awaitItem())
                val errorState = awaitItem()
                assertTrue(errorState is DashboardState.Error)
                assertEquals("Network error", errorState.message)
            }
        }

    // ============================================================================
    // FAKT CALL COUNT VALIDATION TESTS
    // Demonstrates automatic call count tracking by Fakt fakes
    // ============================================================================

    @Test
    fun `GIVEN DashboardViewModel WHEN loading THEN should track call count in fake`() =
        runTest {
            // Given
            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ -> DashboardData(1, 2, 3.0) }
            }

            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When
            viewModel.loadDashboard()
            advanceUntilIdle()

            // Then - Fakt automatically tracks call counts!
            useCase.loadDashboardDataCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    @Test
    fun `GIVEN DashboardViewModel WHEN loading multiple times THEN should track all calls in fake`() =
        runTest {
            // Given
            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ -> DashboardData(1, 2, 3.0) }
            }

            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When
            viewModel.loadDashboard()
            advanceUntilIdle()
            viewModel.loadDashboard()
            advanceUntilIdle()
            viewModel.loadDashboard()
            advanceUntilIdle()

            // Then - Fakt tracks all 3 calls automatically!
            useCase.loadDashboardDataCallCount.test {
                assertEquals(3, awaitItem())
            }
        }

    // ============================================================================
    // REFRESH TESTS
    // ============================================================================

    @Test
    fun `GIVEN Success state WHEN refreshing THEN should reload data`() =
        runTest {
            // Given
            var callCount = 0
            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ ->
                    callCount++
                    DashboardData(callCount, callCount * 2, callCount * 3.0)
                }
            }

            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When - Load first
            viewModel.loadDashboard()
            advanceUntilIdle()

            // Then refresh
            viewModel.refresh()
            advanceUntilIdle()

            // Then - Fakt tracks both calls (load + refresh)
            useCase.loadDashboardDataCallCount.test {
                assertEquals(2, awaitItem())
            }

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is DashboardState.Success)
                assertEquals(2, state.data.activeUsers) // Second call
            }
        }

    @Test
    fun `GIVEN Idle state WHEN refreshing THEN should NOT call use case`() =
        runTest {
            // Given
            val useCase = fakeDashboardUseCase()
            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When - Refresh without loading first
            viewModel.refresh()
            advanceUntilIdle()

            // Then - Should stay Idle and NOT call use case
            viewModel.state.test {
                assertEquals(DashboardState.Idle, awaitItem())
            }

            useCase.loadDashboardDataCallCount.test {
                assertEquals(0, awaitItem()) // No calls to use case
            }
        }

    // ============================================================================
    // CONCURRENCY & THREAD SAFETY TESTS
    // ============================================================================

    @Test
    fun `GIVEN DashboardViewModel WHEN loading 10 times concurrently THEN should be thread safe`() =
        runTest {
            // Given
            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ ->
                    delay(10) // Simulate network delay
                    DashboardData(100, 200, 300.0)
                }
            }

            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When - 10 concurrent loads
            repeat(10) {
                launch { viewModel.loadDashboard() }
            }
            advanceUntilIdle()

            // Then - Fakt tracks all 10 concurrent calls safely!
            useCase.loadDashboardDataCallCount.test {
                assertEquals(10, awaitItem())
            }

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is DashboardState.Success)
            }
        }

    @Test
    fun `GIVEN DashboardViewModel WHEN loading 100 times concurrently THEN should handle high concurrency`() =
        runTest {
            // Given
            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ ->
                    delay(5) // Small delay
                    DashboardData(100, 200, 300.0)
                }
            }

            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When - 100 concurrent loads (stress test!)
            repeat(100) {
                launch { viewModel.loadDashboard() }
            }
            advanceUntilIdle()

            // Then - Fakt handles 100 concurrent calls perfectly!
            useCase.loadDashboardDataCallCount.test {
                assertEquals(100, awaitItem())
            }
        }

    // ============================================================================
    // TIMING & DELAY TESTS
    // ============================================================================

    @Test
    fun `GIVEN DashboardViewModel WHEN loading with delay THEN should advance time correctly`() =
        runTest {
            // Given
            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ ->
                    delay(1000) // 1 second delay
                    DashboardData(100, 200, 300.0)
                }
            }

            val viewModel = factoryDashboardViewModel(useCase = useCase)

            // When
            viewModel.state.test {
                // Initial state
                assertEquals(DashboardState.Idle, awaitItem())

                // Load
                viewModel.loadDashboard()
                runCurrent()

                // Should be Loading (waiting for delay)
                assertEquals(DashboardState.Loading, awaitItem())

                // Advance time by 1 second
                advanceTimeBy(1000)

                // Should now be Success
                val successState = awaitItem()
                assertTrue(successState is DashboardState.Success)
                assertEquals(100, (successState as DashboardState.Success).data.activeUsers)
            }
        }

    // ============================================================================
    // ANALYTICS & LOGGER VALIDATION TESTS
    // ============================================================================

    @Test
    fun `GIVEN DashboardViewModel WHEN loading successfully THEN should track analytics event`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ -> DashboardData(1, 2, 3.0) }
            }

            val viewModel = factoryDashboardViewModel(
                useCase = useCase,
                analytics = analytics,
            )

            // When
            viewModel.loadDashboard()
            advanceUntilIdle()

            // Then
            assertTrue(trackedEvents.contains("dashboard_loaded"))
        }

    @Test
    fun `GIVEN DashboardViewModel WHEN loading fails THEN should track failure analytics event`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics = fakeAnalytics {
                track { eventName, _ ->
                    trackedEvents.add(eventName)
                }
            }

            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ ->
                    throw RuntimeException("Network error")
                }
            }

            val viewModel = factoryDashboardViewModel(
                useCase = useCase,
                analytics = analytics,
            )

            // When
            viewModel.loadDashboard()
            advanceUntilIdle()

            // Then
            assertTrue(trackedEvents.contains("dashboard_load_failed"))
        }

    @Test
    fun `GIVEN DashboardViewModel WHEN loading THEN should log messages`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger = fakeLogger {
                info { message, _ ->
                    loggedMessages.add(message)
                }
            }

            val useCase = fakeDashboardUseCase {
                loadDashboardData { _, _ -> DashboardData(1, 2, 3.0) }
            }

            val viewModel = factoryDashboardViewModel(
                useCase = useCase,
                logger = logger,
            )

            // When
            viewModel.loadDashboard()
            advanceUntilIdle()

            // Then
            assertTrue(loggedMessages.any { it.contains("Loading dashboard data") })
            assertTrue(loggedMessages.any { it.contains("Dashboard loaded successfully") })
        }

    // ============================================================================
    // HELPER FACTORY
    // ============================================================================

    private fun TestScope.factoryDashboardViewModel(
        useCase: DashboardUseCase = fakeDashboardUseCase(),
        analytics: Analytics = fakeAnalytics(),
        logger: Logger = fakeLogger(),
    ) = DashboardViewModel(
        useCase = useCase,
        analytics = analytics,
        logger = logger,
        scope = this,
    )
}
