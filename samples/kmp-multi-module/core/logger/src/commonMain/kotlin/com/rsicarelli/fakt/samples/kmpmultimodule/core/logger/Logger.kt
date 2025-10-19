// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.logger

import com.rsicarelli.fakt.Fake

/**
 * Core logging interface for structured logging across the application.
 *
 * This is a foundational infrastructure service used by all features.
 * In a real app, this would be implemented using a logging framework like Logback, SLF4J, or Napier.
 *
 * Example usage:
 * ```kotlin
 * logger.info("User logged in", mapOf("userId" to user.id))
 * logger.error("Failed to save order", exception, mapOf("orderId" to order.id))
 * ```
 */
@Fake
interface Logger {
    /**
     * Log a debug message with optional structured data.
     */
    fun debug(message: String, data: Map<String, String> = emptyMap())

    /**
     * Log an info message with optional structured data.
     */
    fun info(message: String, data: Map<String, String> = emptyMap())

    /**
     * Log a warning message with optional exception and structured data.
     */
    fun warn(message: String, throwable: Throwable? = null, data: Map<String, String> = emptyMap())

    /**
     * Log an error message with optional exception and structured data.
     */
    fun error(message: String, throwable: Throwable? = null, data: Map<String, String> = emptyMap())

    /**
     * The minimum log level for this logger instance.
     */
    val minLogLevel: LogLevel
}

/**
 * Log levels in order of severity.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}
