// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.notifications

import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Notifications screen state.
 */
sealed class NotificationsState {
    data object Idle : NotificationsState()
    data object Loading : NotificationsState()
    data class Success(
        val notifications: List<Notification>,
        val unreadCount: Int,
    ) : NotificationsState()
    data class Error(val message: String) : NotificationsState()
}

/**
 * Vanilla ViewModel for Notifications feature (no Android dependencies).
 *
 * Demonstrates production-ready patterns:
 * - StateFlow for reactive state management
 * - K2.2+ backing fields pattern (get() = _field)
 * - Thread-safe state updates with .update { }
 * - Optimistic updates pattern for mark-as-read
 * - Badge counter (unread count) automatically calculated
 * - Coroutine scope for async operations
 *
 * This serves as a real-world example for testing with Fakt + Turbine.
 *
 * NOTE: Call counts are automatically tracked by Fakt fakes!
 * Use `notificationUseCase.getNotificationsCallCount` and `markAsReadCallCount` in tests.
 */
class NotificationsViewModel(
    private val notificationUseCase: NotificationUseCase,
    private val userId: String,
    private val storage: KeyValueStorage,
    private val logger: Logger,
    private val scope: CoroutineScope,
) {
    // State - K2.2+ backing field pattern
    private val _state = MutableStateFlow<NotificationsState>(NotificationsState.Idle)
    val state: StateFlow<NotificationsState>
        get() = _state

    /**
     * Load notifications for user.
     * Transitions: Idle -> Loading -> Success/Error
     * Automatically calculates unread count.
     */
    fun loadNotifications() {
        scope.launch {
            try {
                _state.update { NotificationsState.Loading }

                logger.info("Loading notifications for user: $userId")
                val notifications = notificationUseCase.getNotifications(userId, storage, logger)

                val unreadCount = notifications.count { !it.read }

                _state.update { NotificationsState.Success(notifications, unreadCount) }
                logger.info("Notifications loaded: ${notifications.size} total, $unreadCount unread")
            } catch (e: Exception) {
                _state.update { NotificationsState.Error(e.message ?: "Unknown error") }
                logger.error("Failed to load notifications", e)
            }
        }
    }

    /**
     * Mark notification as read with OPTIMISTIC UPDATE pattern.
     *
     * Flow:
     * 1. Immediately update UI to mark as read (Optimistic)
     * 2. Call API to persist the read status
     * 3. On success: keep the optimistic update
     * 4. On failure: rollback to previous state
     */
    fun markAsRead(notificationId: String) {
        scope.launch {
            // Save current state for potential rollback
            val currentState = _state.value
            if (currentState !is NotificationsState.Success) {
                logger.warn("Cannot mark as read: not in Success state")
                return@launch
            }

            try {
                // Find the notification to mark as read
                val notification = currentState.notifications.find { it.id == notificationId }
                if (notification == null) {
                    logger.warn("Notification not found: $notificationId")
                    return@launch
                }

                if (notification.read) {
                    logger.info("Notification already read: $notificationId")
                    return@launch
                }

                // OPTIMISTIC UPDATE - Update UI immediately
                val updatedNotifications =
                    currentState.notifications.map {
                        if (it.id == notificationId) it.copy(read = true) else it
                    }
                val newUnreadCount = updatedNotifications.count { !it.read }

                _state.update { NotificationsState.Success(updatedNotifications, newUnreadCount) }
                logger.info("Optimistically marked notification as read: $notificationId")

                // Persist to backend/storage
                val success = notificationUseCase.markAsRead(notificationId, storage, logger)

                if (!success) {
                    // Rollback on failure
                    _state.update { currentState }
                    logger.warn("Failed to mark as read, rolled back: $notificationId")
                }
            } catch (e: Exception) {
                // Rollback on exception
                _state.update { currentState }
                logger.error("Exception marking as read, rolled back: $notificationId", e)
            }
        }
    }

    /**
     * Refresh notifications (reload from server).
     */
    fun refresh() {
        val currentState = _state.value
        if (currentState is NotificationsState.Success) {
            loadNotifications()
        }
    }

    /**
     * Mark all notifications as read.
     */
    fun markAllAsRead() {
        scope.launch {
            val currentState = _state.value
            if (currentState !is NotificationsState.Success) {
                return@launch
            }

            try {
                logger.info("Marking all notifications as read")

                // OPTIMISTIC UPDATE - Mark all as read in UI
                val updatedNotifications = currentState.notifications.map { it.copy(read = true) }
                _state.update { NotificationsState.Success(updatedNotifications, 0) }

                // Persist all read statuses
                var anyFailed = false
                for (notification in currentState.notifications.filter { !it.read }) {
                    val success = notificationUseCase.markAsRead(notification.id, storage, logger)
                    if (!success) {
                        anyFailed = true
                        break
                    }
                }

                if (anyFailed) {
                    // Rollback on any failure
                    _state.update { currentState }
                    logger.warn("Failed to mark all as read, rolled back")
                }
            } catch (e: Exception) {
                // Rollback on exception
                _state.update { currentState }
                logger.error("Exception marking all as read, rolled back", e)
            }
        }
    }
}
