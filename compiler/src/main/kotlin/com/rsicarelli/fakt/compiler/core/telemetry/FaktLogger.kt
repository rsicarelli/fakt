// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import com.rsicarelli.fakt.compiler.api.LogLevel
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

/**
 * Intelligent logging wrapper for the Fakt compiler plugin.
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
 * logger.debug("Analyzing type parameters") // Only when DEBUG or TRACE
 * logger.trace("IR node: $irNode")         // Only when TRACE
 * ```
 *
 * @property messageCollector The Kotlin compiler's message collector (may be null)
 * @property logLevel The minimum level required for messages to be logged
 */
class FaktLogger(
    private val messageCollector: MessageCollector?,
    val logLevel: LogLevel,
) {
    /**
     * Logs an informational message (concise, production-ready).
     *
     * Shown when level >= INFO (most common, default level).
     * Use for:
     * - High-level compilation summaries
     * - Success/completion messages
     * - Key metrics and statistics
     *
     * **Example:**
     * ```kotlin
     * logger.info("✅ 10 fakes generated in 1.2s")
     * logger.info("📁 Output: build/generated/fakt/commonTest/kotlin")
     * ```
     *
     * @param message The message to log
     */
    fun info(message: String) {
        if (logLevel >= LogLevel.INFO) {
            messageCollector?.report(CompilerMessageSeverity.INFO, "Fakt: $message")
        }
    }

    /**
     * Logs a debug message (detailed breakdown).
     *
     * Shown when level >= DEBUG.
     * Use for:
     * - Per-phase timing breakdowns
     * - Intermediate results and decisions
     * - Cache hit/miss details
     * - Performance insights
     *
     * **Example:**
     * ```kotlin
     * logger.debug("[ANALYSIS] 340ms - PredicateCombiner (18ms)")
     * logger.debug("Cache HIT: UserService (unchanged signature)")
     * ```
     *
     * @param message The message to log
     */
    fun debug(message: String) {
        if (logLevel >= LogLevel.DEBUG) {
            messageCollector?.report(CompilerMessageSeverity.INFO, "Fakt: $message")
        }
    }

    /**
     * Logs a trace message (exhaustive details).
     *
     * Shown only when level == TRACE.
     * Use for:
     * - IR node inspection
     * - Type resolution step-by-step
     * - Generated code snippets
     * - Deep compiler internals
     *
     * **Example:**
     * ```kotlin
     * logger.trace("Extracting type parameters from IR: ${irClass.typeParameters}")
     * logger.trace("Generated implementation (first 5 lines): $codeSnippet")
     * ```
     *
     * @param message The message to log
     */
    fun trace(message: String) {
        if (logLevel >= LogLevel.TRACE) {
            messageCollector?.report(CompilerMessageSeverity.INFO, message)
        }
    }

    /**
     * Logs a warning message.
     *
     * Always shown regardless of level.
     * Use for:
     * - Deprecated API usage
     * - Suboptimal patterns detected
     * - Potential issues that don't prevent compilation
     *
     * **Example:**
     * ```kotlin
     * logger.warn("Interface ComplexService uses unsupported generic pattern")
     * logger.warn("Performance: PairMapper took 150ms (>100ms threshold)")
     * ```
     *
     * @param message The warning message to log
     */
    fun warn(message: String) {
        messageCollector?.report(CompilerMessageSeverity.WARNING, "Fakt: $message")
    }

    /**
     * Logs an error message.
     *
     * Always shown regardless of level.
     * Use for:
     * - Compilation failures
     * - Unsupported constructs
     * - Critical issues that prevent fake generation
     *
     * Errors should include:
     * - What went wrong
     * - Where it happened (interface name, file location)
     * - Why it failed
     * - Suggested fix or workaround
     *
     * **Example:**
     * ```kotlin
     * logger.error("""
     *     Failed to generate fake for ComplexService
     *     Location: com.example.ComplexService (ComplexService.kt:15)
     *     Problem: Unsupported return type Result<List<T>>
     *     Suggestion: Use concrete type or simplify generic structure
     * """.trimIndent())
     * ```
     *
     * @param message The error message to log
     */
    fun error(message: String) {
        messageCollector?.report(CompilerMessageSeverity.ERROR, "Fakt: $message")
    }

    /**
     * Executes a block only if the current log level meets the threshold.
     *
     * Useful for expensive operations that should only run when logging is enabled:
     * ```kotlin
     * logger.ifLevel(LogLevel.TRACE) {
     *     val expensiveDetails = computeIrNodeDetails(irClass)
     *     logger.trace("IR Details: $expensiveDetails")
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
         * Creates a FaktLogger with QUIET level (no output).
         *
         * Useful for tests or when logging should be completely disabled.
         *
         * @param messageCollector Optional message collector (defaults to null)
         * @return A FaktLogger that produces no output
         */
        fun quiet(messageCollector: MessageCollector? = null): FaktLogger = FaktLogger(messageCollector, LogLevel.QUIET)

        /**
         * Creates a FaktLogger with INFO level (default).
         *
         * @param messageCollector The message collector to use
         * @return A FaktLogger with INFO level
         */
        fun info(messageCollector: MessageCollector?): FaktLogger = FaktLogger(messageCollector, LogLevel.INFO)

        /**
         * Creates a FaktLogger with DEBUG level.
         *
         * @param messageCollector The message collector to use
         * @return A FaktLogger with DEBUG level
         */
        fun debug(messageCollector: MessageCollector?): FaktLogger = FaktLogger(messageCollector, LogLevel.DEBUG)

        /**
         * Creates a FaktLogger with TRACE level.
         *
         * @param messageCollector The message collector to use
         * @return A FaktLogger with TRACE level
         */
        fun trace(messageCollector: MessageCollector?): FaktLogger = FaktLogger(messageCollector, LogLevel.TRACE)
    }
}
