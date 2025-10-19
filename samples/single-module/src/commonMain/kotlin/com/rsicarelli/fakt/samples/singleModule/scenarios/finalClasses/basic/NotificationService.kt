// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses

import com.rsicarelli.fakt.Fake

/**
 * Example: Abstract class with abstract and open methods
 * Common in frameworks (ViewModels, Services, etc.)
 */
@Fake
abstract class NotificationService {
    // Abstract method - must be overridden
    abstract fun sendNotification(
        userId: String,
        message: String,
    )

    // Open method with default implementation
    open fun formatMessage(message: String): String = "[NOTIFICATION] $message"

    // Final method - cannot be overridden
    fun logNotification(
        userId: String,
        message: String,
    ) {
        println("Notification sent to $userId: $message")
    }
}
