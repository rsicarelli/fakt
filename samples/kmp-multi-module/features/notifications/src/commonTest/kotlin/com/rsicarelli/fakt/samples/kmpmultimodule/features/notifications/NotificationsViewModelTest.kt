// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.rsicarelli.fakt.samples.kmpmultimodule.features.notifications

import app.cash.turbine.test
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeKeyValueStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for NotificationsViewModel demonstrating:
 * - Turbine for StateFlow testing
 * - K2.2+ backing fields pattern
 * - OPTIMISTIC UPDATES pattern (update UI before API)
 * - Rollback on error
 * - Badge counter tracking (unread count)
 * - Concurrency and thread safety
 * - Factory pattern with TestScope extension
 *
 * This serves as a production-ready example for the Fakt community.
 */
class NotificationsViewModelTest {

    companion object {
        // Helper to create test notification
        private fun createTestNotification(
            id: String = "notif-1",
            title: String = "Test Title",
            body: String = "Test Body",
            timestamp: Long = 1234567890L,
            read: Boolean = false,
        ) = Notification(
            id = id,
            title = title,
            body = body,
            timestamp = timestamp,
            read = read,
        )
    }

    // ============================================================================
    // LOAD NOTIFICATIONS TESTS
    // ============================================================================

    @Test
    fun `GIVEN valid userId WHEN loading notifications THEN should transition to Success with unread count`() =
        runTest {
            // Given
            val notifications =
                listOf(
                    createTestNotification(id = "1", read = false),
                    createTestNotification(id = "2", read = true),
                    createTestNotification(id = "3", read = false),
                )

            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> notifications }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // When
            viewModel.state.test {
                assertEquals(NotificationsState.Idle, awaitItem())

                viewModel.loadNotifications()
                advanceUntilIdle()

                assertEquals(NotificationsState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is NotificationsState.Success)
                assertEquals(3, successState.notifications.size)
                assertEquals(2, successState.unreadCount) // 2 unread notifications
            }
        }

    @Test
    fun `GIVEN empty notifications WHEN loading THEN should show Success with zero unread count`() =
        runTest {
            // Given
            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> emptyList() }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // When
            viewModel.loadNotifications()
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is NotificationsState.Success)
                assertEquals(0, state.notifications.size)
                assertEquals(0, state.unreadCount)
            }
        }

    @Test
    fun `GIVEN API failure WHEN loading notifications THEN should show Error state`() =
        runTest {
            // Given
            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ ->
                        throw RuntimeException("Network error")
                    }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // When
            viewModel.loadNotifications()
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is NotificationsState.Error)
                assertEquals("Network error", state.message)
            }
        }

    // ============================================================================
    // MARK AS READ TESTS
    // ============================================================================

    @Test
    fun `GIVEN unread notification WHEN marking as read THEN should update optimistically and reduce unread count`() =
        runTest {
            // Given
            val notifications =
                listOf(
                    createTestNotification(id = "1", read = false),
                    createTestNotification(id = "2", read = false),
                )

            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> notifications }
                    markAsRead { _, _, _ -> true }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // Load notifications first
            viewModel.loadNotifications()
            advanceUntilIdle()

            // When - Mark notification "1" as read
            viewModel.state.test {
                val initialState = awaitItem()
                assertTrue(initialState is NotificationsState.Success)
                assertEquals(2, initialState.unreadCount)

                viewModel.markAsRead("1")
                advanceUntilIdle()

                // Then - Optimistically updated
                val updatedState = awaitItem()
                assertTrue(updatedState is NotificationsState.Success)
                assertEquals(1, updatedState.unreadCount) // Reduced by 1
                assertTrue(updatedState.notifications.first { it.id == "1" }.read)
            }
        }

    @Test
    fun `GIVEN mark as read failure WHEN marking as read THEN should rollback to previous state`() =
        runTest {
            // Given
            val notifications =
                listOf(
                    createTestNotification(id = "1", read = false),
                )

            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> notifications }
                    markAsRead { _, _, _ -> false } // Mark as read fails
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // Load notifications first
            viewModel.loadNotifications()
            advanceUntilIdle()

            // When - Mark as read (will fail)
            viewModel.state.test {
                val initialState = awaitItem()
                assertTrue(initialState is NotificationsState.Success)
                assertEquals(1, initialState.unreadCount)

                viewModel.markAsRead("1")
                advanceUntilIdle()

                // Optimistic update
                val optimisticState = awaitItem()
                assertTrue(optimisticState is NotificationsState.Success)
                assertEquals(0, optimisticState.unreadCount)

                // Then - Rollback after failure
                val rolledBackState = awaitItem()
                assertTrue(rolledBackState is NotificationsState.Success)
                assertEquals(1, rolledBackState.unreadCount) // Rolled back!
            }
        }

    @Test
    fun `GIVEN already read notification WHEN marking as read THEN should not update state`() =
        runTest {
            // Given
            val notifications =
                listOf(
                    createTestNotification(id = "1", read = true), // Already read
                )

            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> notifications }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // Load notifications first
            viewModel.loadNotifications()
            advanceUntilIdle()

            // When - Try to mark already read notification
            viewModel.state.test {
                val initialState = awaitItem()
                assertTrue(initialState is NotificationsState.Success)
                assertEquals(0, initialState.unreadCount)

                viewModel.markAsRead("1")
                advanceUntilIdle()

                // Then - No state update (expectNone would timeout if state changed)
            }
        }

    // ============================================================================
    // MARK ALL AS READ TESTS
    // ============================================================================

    @Test
    fun `GIVEN multiple unread notifications WHEN marking all as read THEN should update all and set unread count to zero`() =
        runTest {
            // Given
            val notifications =
                listOf(
                    createTestNotification(id = "1", read = false),
                    createTestNotification(id = "2", read = false),
                    createTestNotification(id = "3", read = true),
                )

            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> notifications }
                    markAsRead { _, _, _ -> true }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // Load notifications first
            viewModel.loadNotifications()
            advanceUntilIdle()

            // When - Mark all as read
            viewModel.state.test {
                val initialState = awaitItem()
                assertTrue(initialState is NotificationsState.Success)
                assertEquals(2, initialState.unreadCount)

                viewModel.markAllAsRead()
                advanceUntilIdle()

                // Then - All marked as read
                val updatedState = awaitItem()
                assertTrue(updatedState is NotificationsState.Success)
                assertEquals(0, updatedState.unreadCount)
                assertTrue(updatedState.notifications.all { it.read })
            }
        }

    // ============================================================================
    // CONCURRENCY TESTS
    // ============================================================================

    @Test
    fun `GIVEN NotificationsViewModel WHEN 10 concurrent mark-as-read operations THEN should be thread safe`() =
        runTest {
            // Given
            val notifications =
                (1..10).map { createTestNotification(id = it.toString(), read = false) }

            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> notifications }
                    markAsRead { _, _, _ ->
                        delay(10)
                        true
                    }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // Load notifications first
            viewModel.loadNotifications()
            advanceUntilIdle()

            // When - 10 concurrent mark-as-read
            repeat(10) { index ->
                launch {
                    viewModel.markAsRead((index + 1).toString())
                }
            }
            advanceUntilIdle()

            // Then - Fakt tracks all 10 calls!
            notificationUseCase.markAsReadCallCount.test {
                assertEquals(10, awaitItem())
            }
        }

    // ============================================================================
    // REFRESH TESTS
    // ============================================================================

    @Test
    fun `GIVEN Success state WHEN refreshing THEN should reload notifications`() =
        runTest {
            // Given
            var callCount = 0
            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ ->
                        callCount++
                        listOf(createTestNotification(id = callCount.toString()))
                    }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // Load notifications first
            viewModel.loadNotifications()
            advanceUntilIdle()

            // When - Refresh
            viewModel.refresh()
            advanceUntilIdle()

            // Then - Fakt tracks both calls (load + refresh)
            notificationUseCase.getNotificationsCallCount.test {
                assertEquals(2, awaitItem())
            }
        }

    // ============================================================================
    // FAKT CALL COUNT TESTS
    // ============================================================================

    @Test
    fun `GIVEN NotificationsViewModel WHEN loading notifications THEN should track getNotifications call count`() =
        runTest {
            // Given
            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> listOf(createTestNotification()) }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // When
            viewModel.loadNotifications()
            advanceUntilIdle()

            // Then - Fakt tracks the call automatically!
            notificationUseCase.getNotificationsCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    @Test
    fun `GIVEN NotificationsViewModel WHEN marking as read THEN should track markAsRead call count`() =
        runTest {
            // Given
            val notifications = listOf(createTestNotification(id = "1", read = false))

            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> notifications }
                    markAsRead { _, _, _ -> true }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase)

            // Load notifications first
            viewModel.loadNotifications()
            advanceUntilIdle()

            // When - Mark as read
            viewModel.markAsRead("1")
            advanceUntilIdle()

            // Then - Fakt tracks it!
            notificationUseCase.markAsReadCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    // ============================================================================
    // LOGGER VALIDATION TESTS
    // ============================================================================

    @Test
    fun `GIVEN NotificationsViewModel WHEN executing operations THEN should log messages`() =
        runTest {
            // Given
            val loggedMessages = mutableListOf<String>()
            val logger =
                fakeLogger {
                    info { message, _ ->
                        loggedMessages.add(message)
                    }
                }

            val notificationUseCase =
                fakeNotificationUseCase {
                    getNotifications { _, _, _ -> listOf(createTestNotification()) }
                }

            val viewModel = factoryNotificationsViewModel(notificationUseCase = notificationUseCase, logger = logger)

            // When
            viewModel.loadNotifications()
            advanceUntilIdle()

            // Then
            assertTrue(loggedMessages.any { it.contains("Loading notifications") })
            assertTrue(loggedMessages.any { it.contains("Notifications loaded") })
        }

    // ============================================================================
    // HELPER FACTORY
    // ============================================================================

    private fun TestScope.factoryNotificationsViewModel(
        notificationUseCase: NotificationUseCase = fakeNotificationUseCase(),
        userId: String = "test-user-123",
        storage: KeyValueStorage = fakeKeyValueStorage(),
        logger: Logger = fakeLogger(),
    ) = NotificationsViewModel(
        notificationUseCase = notificationUseCase,
        userId = userId,
        storage = storage,
        logger = logger,
        scope = this,
    )
}
