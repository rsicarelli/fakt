// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch085

import com.rsicarelli.fakt.Fake

@Fake
abstract class NotificationService_finalClasses_basic8498 {
    
    abstract fun sendNotification(
        userId: String,
        message: String,
    )

    
    open fun formatMessage(message: String): String = "[NOTIFICATION] $message"

    
    fun logNotification(
        userId: String,
        message: String,
    ) {
        println("Notification sent to $userId: $message")
    }
}
