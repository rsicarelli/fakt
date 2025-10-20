// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

/**
 * Utility for formatting nanosecond durations to human-readable strings.
 *
 * Provides consistent time formatting across the Fakt compiler plugin's telemetry system.
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
 * - Consistent across all telemetry components
 * - Performance optimized (simple arithmetic, no string allocations until final format)
 */
object TimeFormatter {
    /**
     * Formats nanoseconds to human-readable time string with smart unit selection.
     *
     * @param nanos Duration in nanoseconds
     * @return Formatted time string (e.g., "234µs", "45ms", "1.2s", "1m 5s")
     */
    fun format(nanos: Long): String {
        val micros = nanos / 1_000
        val millis = nanos / 1_000_000
        val seconds = nanos / 1_000_000_000.0

        return when {
            nanos < 1_000_000 -> "${micros}µs" // < 1ms → show µs
            millis < 1_000 -> "${millis}ms" // 1-999ms → show ms
            seconds < 60 -> String.format("%.1fs", seconds) // 1-59s → show decimal seconds
            else -> {
                // 60s+ → show minutes and seconds
                val minutes = millis / 60_000
                val remainingSeconds = (millis % 60_000) / 1_000
                "${minutes}m ${remainingSeconds}s"
            }
        }
    }
}
