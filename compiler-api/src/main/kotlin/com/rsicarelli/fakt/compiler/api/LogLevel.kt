// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

/**
 * Logging verbosity levels for the Fakt compiler plugin.
 *
 * Controls how much information is displayed during compilation:
 * - [QUIET]: No output except errors (fastest compilation, minimal noise)
 * - [INFO]: Concise summary with key metrics (default, production-ready)
 * - [DEBUG]: Detailed breakdown with FIR + IR details and type resolution (troubleshooting)
 *
 * **Usage in build.gradle.kts (Type-Safe!):**
 * ```kotlin
 * import com.rsicarelli.fakt.compiler.api.LogLevel
 *
 * fakt {
 *     logLevel.set(LogLevel.INFO)    // ✅ Type-safe!
 *     logLevel.set(LogLevel.DEBUG)   // ✅ IDE autocomplete
 *     logLevel.set(LogLevel.QUIET)   // ✅ Compile-time validation
 * }
 * ```
 *
 * **Performance Impact:**
 * - QUIET: Zero overhead (recommended for CI/CD)
 * - INFO: Negligible overhead (<1ms)
 * - DEBUG: Minor overhead (~5-10ms for detailed logging)
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
     * - FIR + IR node inspection details
     * - Type resolution steps
     * - Generic pattern analysis breakdown
     * - Import resolution process
     * - Source set mapping details
     * - Generated code snippets (first few lines)
     *
     * Typical output: 30-100+ lines
     *
     * Use when:
     * - Troubleshooting generation issues
     * - Understanding performance bottlenecks
     * - Investigating why specific interfaces take long
     * - Deep debugging compiler plugin issues
     * - Understanding FIR + IR generation internals
     * - Reporting bugs with full context
     * - Developing new features
     */
    DEBUG,
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
                else -> INFO // Default to INFO on invalid input
            }
    }
}
