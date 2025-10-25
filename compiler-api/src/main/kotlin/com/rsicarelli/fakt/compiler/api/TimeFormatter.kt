// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

import java.util.Locale

/**
 * Utility for formatting nanosecond durations to human-readable strings.
 *
 * Provides consistent time formatting across Fakt compiler plugin and Gradle plugin.
 * Automatically selects the most appropriate unit based on duration magnitude.
 *
 * **Unit Selection:**
 * - 0-999µs: Microseconds (e.g., "234µs", "12µs")
 * - 1-999ms: Milliseconds (e.g., "45ms", "150ms")
 * - 1-59s: Seconds with decimal (e.g., "1.2s", "5.8s")
 * - 60s+: Minutes and seconds (e.g., "1m 5s", "2m 30s")
 *
 * **Usage:**
 * ```kotlin
 * val startTime = System.nanoTime()
 * // ... operation ...
 * val duration = System.nanoTime() - startTime
 * val formatted = TimeFormatter.format(duration)  // e.g., "234µs"
 * ```
 *
 * **Design:**
 * - Single responsibility: format nanoseconds to human-readable strings
 * - No state (object singleton)
 * - Consistent across all Fakt components
 * - Performance optimized (simple arithmetic, no string allocations until final format)
 */
object TimeFormatter {
    // Time conversion constants
    private const val NANOS_PER_MICRO = 1_000L
    private const val NANOS_PER_MILLI = 1_000_000L
    private const val NANOS_PER_SECOND = 1_000_000_000L
    private const val MILLIS_PER_SECOND = 1_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_THRESHOLD = 1_000L
    private const val SECONDS_PER_MINUTE = 60

    /**
     * Formats nanoseconds to human-readable time string with smart unit selection.
     *
     * @param nanos Duration in nanoseconds
     * @return Formatted time string (e.g., "234µs", "45ms", "1.2s", "1m 5s")
     */
    fun format(nanos: Long): String {
        val micros = nanos / NANOS_PER_MICRO
        val millis = nanos / NANOS_PER_MILLI
        val seconds = nanos / NANOS_PER_SECOND.toDouble()

        return when {
            nanos < NANOS_PER_MILLI -> "${micros}µs" // < 1ms → show µs
            millis < MILLIS_THRESHOLD -> "${millis}ms" // 1-999ms → show ms
            seconds < SECONDS_PER_MINUTE ->
                String.format(Locale.ROOT, "%.1fs", seconds) // 1-59s → show decimal seconds
            else -> {
                // 60s+ → show minutes and seconds
                val minutes = millis / MILLIS_PER_MINUTE
                val remainingSeconds = (millis % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND
                "${minutes}m ${remainingSeconds}s"
            }
        }
    }
}
