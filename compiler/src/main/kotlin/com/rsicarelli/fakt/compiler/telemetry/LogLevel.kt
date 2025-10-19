// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

/**
 * Logging verbosity levels for the Fakt compiler plugin.
 *
 * Controls how much information is displayed during compilation:
 * - [QUIET]: No output except errors (fastest compilation, minimal noise)
 * - [INFO]: Concise summary with key metrics (default, production-ready)
 * - [DEBUG]: Detailed breakdown by compilation phase (troubleshooting)
 * - [TRACE]: Exhaustive details including IR nodes and type resolution (deep debugging)
 *
 * **Usage in build.gradle.kts:**
 * ```kotlin
 * fakt {
 *     logLevel.set("INFO")    // Default - concise summary
 *     logLevel.set("DEBUG")   // Detailed phase breakdown
 *     logLevel.set("TRACE")   // Everything (for deep debugging)
 *     logLevel.set("QUIET")   // Silent (only errors)
 * }
 * ```
 *
 * **Performance Impact:**
 * - QUIET: Zero overhead (recommended for CI/CD)
 * - INFO: Negligible overhead (<1ms)
 * - DEBUG: Minor overhead (~5-10ms for aggregation)
 * - TRACE: Moderate overhead (~20-50ms for detailed logging)
 *
 * The enum is comparable, allowing checks like `currentLevel >= LogLevel.DEBUG`.
 */
enum class LogLevel {
    /**
     * No output except errors.
     *
     * Use when:
     * - Running in CI/CD for fast builds
     * - You don't need compilation feedback
     * - Minimizing build noise is critical
     *
     * Output: Nothing (errors still shown via ERROR severity)
     */
    QUIET,

    /**
     * Concise summary with key metrics (default).
     *
     * Shows:
     * - Total fakes generated
     * - Compilation time
     * - Cache hit rate
     * - Output directory
     *
     * Typical output: 5-10 lines
     *
     * Use when:
     * - Normal development (default)
     * - You want to know what happened without details
     * - Production builds with some visibility
     */
    INFO,

    /**
     * Detailed breakdown by compilation phase.
     *
     * Shows everything from INFO plus:
     * - Per-phase timing (discovery, analysis, generation, I/O)
     * - Per-interface summary (name, pattern, timing)
     * - Cache statistics (hits, misses, reasons)
     * - Slow operation warnings
     *
     * Typical output: 30-50 lines
     *
     * Use when:
     * - Troubleshooting generation issues
     * - Understanding performance bottlenecks
     * - Investigating why specific interfaces take long
     */
    DEBUG,

    /**
     * Exhaustive details for deep debugging.
     *
     * Shows everything from DEBUG plus:
     * - IR node inspection details
     * - Type resolution steps
     * - Generic pattern analysis breakdown
     * - Import resolution process
     * - Source set mapping details
     * - Generated code snippets (first few lines)
     *
     * Typical output: 100-200+ lines
     *
     * Use when:
     * - Deep debugging compiler plugin issues
     * - Understanding IR generation internals
     * - Reporting bugs with full context
     * - Developing new features
     */
    TRACE,
    ;

    companion object {
        /**
         * Parses a string into a [LogLevel], with fallback to INFO on invalid input.
         *
         * Case-insensitive. Handles common variations:
         * - "INFO", "info", "Info" → LogLevel.INFO
         * - "DEBUG", "debug" → LogLevel.DEBUG
         * - Invalid values → LogLevel.INFO (default)
         *
         * @param value The string representation of the log level
         * @return The corresponding LogLevel, or INFO if parsing fails
         *
         * **Example:**
         * ```kotlin
         * LogLevel.fromString("DEBUG")  // → LogLevel.DEBUG
         * LogLevel.fromString("invalid") // → LogLevel.INFO (with fallback)
         * ```
         */
        fun fromString(value: String?): LogLevel =
            when (value?.uppercase()) {
                "QUIET" -> QUIET
                "INFO" -> INFO
                "DEBUG" -> DEBUG
                "TRACE" -> TRACE
                else -> INFO // Default to INFO on invalid input
            }
    }
}
