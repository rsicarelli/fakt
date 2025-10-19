// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.notifications

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.WebSocketClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage

// Notifications feature - Push notifications and real-time updates
data class Notification(val id: String, val title: String, val body: String, val timestamp: Long, val read: Boolean)

@Fake
interface NotificationUseCase {
    suspend fun getNotifications(userId: String, storage: KeyValueStorage, logger: Logger): List<Notification>

    suspend fun markAsRead(notificationId: String, storage: KeyValueStorage, logger: Logger): Boolean

    suspend fun subscribeToNotifications(userId: String, webSocket: WebSocketClient, logger: Logger)
}
