// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

/**
 * Result of a timed operation containing both the return value and duration.
 *
 * @param T The type of the operation result
 * @property result The return value from the timed block
 * @property durationNanos The execution time in nanoseconds
 */
data class TimedResult<T>(
    val result: T,
    val durationNanos: Long,
)

/**
 * Measures execution time of a block in nanoseconds.
 *
 * Returns both the block's result and the duration, eliminating the need for
 * manual `System.nanoTime()` calls and duration calculations.
 *
 * **Usage:**
 * ```kotlin
 * val (result, duration) = measureTimeNanos {
 *     // expensive operation
 *     processData()
 * }
 * logger.debug("Processed data in ${TimeFormatter.format(duration)}")
 * ```
 *
 * **For side-effect blocks (Unit return):**
 * ```kotlin
 * val (_, duration) = measureTimeNanos {
 *     validateInterface()
 * }
 * logger.debug("Validation took ${TimeFormatter.format(duration)}")
 * ```
 *
 * @param block The operation to time
 * @return TimedResult containing the block's result and execution duration
 */
inline fun <T> measureTimeNanos(block: () -> T): TimedResult<T> {
    val startTime = System.nanoTime()
    val result = block()
    val duration = System.nanoTime() - startTime
    return TimedResult(result, duration)
}
