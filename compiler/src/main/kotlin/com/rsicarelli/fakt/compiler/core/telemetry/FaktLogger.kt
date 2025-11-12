// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import com.rsicarelli.fakt.compiler.api.LogLevel
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

/**
 * Logging wrapper for the Fakt compiler plugin.
 *
 * Wraps the Kotlin compiler's [MessageCollector] with level-aware logging.
 * Only outputs messages that meet or exceed the configured [logLevel], preventing
 * log spam while maintaining debugging capabilities when needed.
 *
 * **Design:**
 * - Wraps standard Kotlin compiler MessageCollector API
 * - Level-aware: filters messages based on configured threshold
 * - Zero overhead when QUIET (messages not even formatted)
 * - Thread-safe (delegates to MessageCollector which is thread-safe)
 * - Null-safe (handles null MessageCollector gracefully)
 *
 * **Usage:**
 * ```kotlin
 * val logger = FaktLogger(messageCollector, LogLevel.DEBUG)
 *
 * logger.info("Processing interface")      // Always shown (level >= INFO)
 * logger.debug("Analyzing type parameters") // Only when DEBUG
 * ```
 *
 * @property messageCollector The Kotlin compiler's message collector (may be null)
 * @property logLevel The minimum level required for messages to be logged
 */
class FaktLogger(
    private val messageCollector: MessageCollector?,
    val logLevel: LogLevel,
) {

    fun info(message: String) {
        if (logLevel >= LogLevel.INFO) {
            messageCollector?.report(CompilerMessageSeverity.INFO, message)
        }
    }

    fun debug(message: String) {
        if (logLevel >= LogLevel.DEBUG) {
            messageCollector?.report(CompilerMessageSeverity.INFO, message)
        }
    }

    fun warn(message: String) {
        messageCollector?.report(CompilerMessageSeverity.WARNING, message)
    }

    fun error(message: String) {
        messageCollector?.report(CompilerMessageSeverity.ERROR, message)
    }

    inline fun ifLevel(
        level: LogLevel,
        block: () -> Unit,
    ) {
        if (logLevel >= level) {
            block()
        }
    }

    companion object {
        fun quiet(messageCollector: MessageCollector? = null): FaktLogger =
            FaktLogger(messageCollector, LogLevel.QUIET)

        fun info(messageCollector: MessageCollector?): FaktLogger =
            FaktLogger(messageCollector, LogLevel.INFO)

        fun debug(messageCollector: MessageCollector?): FaktLogger =
            FaktLogger(messageCollector, LogLevel.DEBUG)
    }
}
