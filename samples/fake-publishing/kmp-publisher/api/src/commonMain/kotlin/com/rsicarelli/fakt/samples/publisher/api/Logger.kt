// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.publisher.api

import com.rsicarelli.fakt.Fake

/**
 * Open logger class demonstrating fake generation for open classes.
 * This tests open class handling with default implementations.
 */
@Fake
open class Logger {
    /**
     * The minimum log level for this logger.
     */
    open val level: String
        get() = "INFO"

    /**
     * Logs a message at INFO level.
     *
     * @param message The message to log
     */
    open fun log(message: String) {
        println("[$level] $message")
    }

    /**
     * Logs an error message.
     *
     * @param message The error message
     * @param throwable Optional throwable to log
     */
    open fun error(message: String, throwable: Throwable? = null) {
        println("[ERROR] $message")
        throwable?.printStackTrace()
    }
}
