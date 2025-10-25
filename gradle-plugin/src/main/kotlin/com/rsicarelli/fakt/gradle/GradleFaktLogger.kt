// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.LogLevel
import org.gradle.api.logging.Logger

/**
 * Intelligent logging wrapper for Fakt Gradle tasks.
 *
 * Wraps Gradle's [Logger] with level-aware logging, matching the behavior of
 * FaktLogger in the compiler plugin for consistent user experience.
 *
 * **Design:**
 * - Wraps standard Gradle Logger API
 * - Level-aware: filters messages based on configured threshold
 * - Zero overhead when QUIET (messages not even formatted)
 * - Thread-safe (delegates to Logger which is thread-safe)
 * - Automatically prefixes messages with "Fakt:" for consistency
 *
 * **Usage:**
 * ```kotlin
 * val logger = GradleFaktLogger(task.logger, logLevel.get())
 *
 * logger.info("Collected 10 fakes")      // Always shown (level >= INFO)
 * logger.debug("Processing source set") // Only when DEBUG or TRACE
 * logger.trace("File details: $file")   // Only when TRACE
 * ```
 *
 * **Log Level Mapping:**
 * - QUIET: No output (all messages suppressed)
 * - INFO: lifecycle() for summaries (always visible)
 * - DEBUG: info() for detailed info
 * - TRACE: debug() for exhaustive details
 *
 * @property logger The Gradle logger instance
 * @property logLevel The minimum level required for messages to be logged
 */
class GradleFaktLogger(
    private val logger: Logger,
    val logLevel: LogLevel,
) {
    /**
     * Logs an informational message (concise, production-ready).
     *
     * Shown when level >= INFO (most common, default level).
     * Uses Gradle lifecycle() for high visibility.
     *
     * Use for:
     * - High-level task summaries
     * - Success/completion messages
     * - Key metrics and statistics
     *
     * **Example:**
     * ```kotlin
     * logger.info("Collected 10 fakes from notifications")
     * logger.info("Platform distribution: commonMain (8), jvmMain (2)")
     * ```
     *
     * @param message The message to log (without "Fakt:" prefix - added automatically)
     */
    fun info(message: String) {
        if (logLevel >= LogLevel.INFO) {
            logger.lifecycle("Fakt: $message")
        }
    }

    /**
     * Logs a debug message (detailed breakdown).
     *
     * Shown when level >= DEBUG.
     * Uses Gradle info() for moderate visibility.
     *
     * Use for:
     * - Per-source-set details
     * - File collection progress
     * - Platform detection results
     * - Performance insights
     *
     * **Example:**
     * ```kotlin
     * logger.debug("Collected 5 files from commonTest")
     * logger.debug("Platform detection: api.ios.* → iosMain")
     * ```
     *
     * @param message The message to log (without "Fakt:" prefix - added automatically)
     */
    fun debug(message: String) {
        if (logLevel >= LogLevel.DEBUG) {
            logger.info("Fakt: $message")
        }
    }

    /**
     * Logs a trace message (exhaustive details).
     *
     * Shown only when level == TRACE.
     * Uses Gradle debug() for low visibility (only with --debug flag).
     *
     * Use for:
     * - File-by-file processing details
     * - Package analysis step-by-step
     * - Source set registration details
     * - Deep Gradle internals
     *
     * **Example:**
     * ```kotlin
     * logger.trace("Analyzing package: api.ios.notifications")
     * logger.trace("Registering iosMain with platform-specific fakes")
     * ```
     *
     * @param message The message to log (without "Fakt:" prefix - added automatically)
     */
    fun trace(message: String) {
        if (logLevel >= LogLevel.TRACE) {
            logger.debug("Fakt: $message")
        }
    }

    /**
     * Logs a warning message.
     *
     * Always shown regardless of level.
     * Uses Gradle warn() for high visibility.
     *
     * Use for:
     * - Missing source directories
     * - No fakes found
     * - Suboptimal configurations
     * - Potential issues that don't prevent execution
     *
     * **Example:**
     * ```kotlin
     * logger.warn("No fakes found in source module 'foundation'")
     * logger.warn("Cache unavailable: Failed to load signatures")
     * ```
     *
     * @param message The warning message to log (without "Fakt:" prefix - added automatically)
     */
    fun warn(message: String) {
        logger.warn("Fakt: $message")
    }

    /**
     * Logs an error message.
     *
     * Always shown regardless of level.
     * Uses Gradle error() for maximum visibility.
     *
     * Use for:
     * - Task failures
     * - File I/O errors
     * - Configuration errors
     * - Critical issues that prevent execution
     *
     * **Example:**
     * ```kotlin
     * logger.error("Failed to collect fakes: source directory not accessible")
     * ```
     *
     * @param message The error message to log (without "Fakt:" prefix - added automatically)
     */
    fun error(message: String) {
        logger.error("Fakt: $message")
    }

    /**
     * Executes a block only if the current log level meets the threshold.
     *
     * Useful for expensive operations that should only run when logging is enabled:
     * ```kotlin
     * logger.ifLevel(LogLevel.TRACE) {
     *     val expensiveDetails = analyzeAllFiles(sourceDir)
     *     logger.trace("File analysis: $expensiveDetails")
     * }
     * ```
     *
     * @param level The minimum level required to execute the block
     * @param block The code to execute if level is met
     */
    inline fun ifLevel(
        level: LogLevel,
        block: () -> Unit,
    ) {
        if (logLevel >= level) {
            block()
        }
    }

    companion object {
        /**
         * Creates a GradleFaktLogger with QUIET level (no output).
         *
         * @param logger The Gradle logger instance
         * @return A GradleFaktLogger that produces no output
         */
        fun quiet(logger: Logger): GradleFaktLogger = GradleFaktLogger(logger, LogLevel.QUIET)

        /**
         * Creates a GradleFaktLogger with INFO level (default).
         *
         * @param logger The Gradle logger instance
         * @return A GradleFaktLogger with INFO level
         */
        fun info(logger: Logger): GradleFaktLogger = GradleFaktLogger(logger, LogLevel.INFO)

        /**
         * Creates a GradleFaktLogger with DEBUG level.
         *
         * @param logger The Gradle logger instance
         * @return A GradleFaktLogger with DEBUG level
         */
        fun debug(logger: Logger): GradleFaktLogger = GradleFaktLogger(logger, LogLevel.DEBUG)

        /**
         * Creates a GradleFaktLogger with TRACE level.
         *
         * @param logger The Gradle logger instance
         * @return A GradleFaktLogger with TRACE level
         */
        fun trace(logger: Logger): GradleFaktLogger = GradleFaktLogger(logger, LogLevel.TRACE)
    }
}
