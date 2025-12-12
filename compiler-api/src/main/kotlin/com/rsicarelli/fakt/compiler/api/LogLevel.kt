// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

/**
 * Logging verbosity levels for the Fakt compiler plugin.
 *
 * Controls compilation output:
 * - [QUIET]: No output except errors (CI/CD builds)
 * - [INFO]: Concise summary with key metrics (default)
 * - [DEBUG]: Detailed FIR + IR phase timing (troubleshooting)
 *
 * **Usage:**
 * ```kotlin
 * import com.rsicarelli.fakt.compiler.api.LogLevel
 *
 * fakt {
 *     logLevel.set(LogLevel.INFO)
 * }
 * ```
 */
enum class LogLevel {
    /**
     * No output except errors. Use for CI/CD pipelines and production builds.
     */
    QUIET,

    /**
     * Concise summary with key metrics (default).
     *
     * Use for local development and monitoring cache effectiveness.
     *
     * **Example output:**
     * ```
     * Fakt: 101 fakes generated in 35ms (50 cached)
     *   Interfaces: 101 | Classes: 0
     *   FIR: 6ms | IR: 29ms
     *   Cache: 50/101 (49%)
     * ```
     */
    INFO,

    /**
     * Detailed FIR + IR phase timing.
     *
     * Use for troubleshooting, performance analysis, and bug reports.
     *
     * **Example output:**
     * ```
     * FIR + IR trace
     * ├─ Total FIR time: 6ms
     * ├─ Total IR time: 58ms
     * │  ├─ FIR analysis: 1 type parameters, 6 members (55µs)
     * │  └─ IR generation: FakeDataCacheImpl 83 LOC (766µs)
     * ```
     */
    DEBUG,
    ;

    companion object {
        /**
         * Parses a string to [LogLevel] (case-insensitive).
         *
         * @return The corresponding LogLevel, or INFO if invalid
         */
        fun fromString(value: String?): LogLevel =
            when (value?.uppercase()) {
                "QUIET" -> QUIET
                "INFO" -> INFO
                "DEBUG" -> DEBUG
                else -> INFO
            }
    }
}
