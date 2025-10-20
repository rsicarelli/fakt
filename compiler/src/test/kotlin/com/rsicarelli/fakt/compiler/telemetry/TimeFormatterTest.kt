// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.telemetry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

/**
 * Tests for TimeFormatter utility.
 *
 * Validates time formatting at different duration ranges:
 * - Microseconds (0-999µs)
 * - Milliseconds (1-999ms)
 * - Seconds (1-59s)
 * - Minutes (60s+)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimeFormatterTest {
    // ════════════════════════════════════════
    // Microseconds Range (< 1ms)
    // ════════════════════════════════════════

    @Test
    fun `GIVEN 0 nanoseconds WHEN formatting THEN should return 0µs`() {
        // GIVEN
        val nanos = 0L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("0µs", formatted)
    }

    @Test
    fun `GIVEN 1 microsecond WHEN formatting THEN should return 1µs`() {
        // GIVEN
        val nanos = 1_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1µs", formatted)
    }

    @Test
    fun `GIVEN 234 microseconds WHEN formatting THEN should return 234µs`() {
        // GIVEN
        val nanos = 234_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("234µs", formatted)
    }

    @Test
    fun `GIVEN 999 microseconds WHEN formatting THEN should return 999µs`() {
        // GIVEN
        val nanos = 999_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("999µs", formatted)
    }

    // ════════════════════════════════════════
    // Milliseconds Range (1-999ms)
    // ════════════════════════════════════════

    @Test
    fun `GIVEN 1 millisecond WHEN formatting THEN should return 1ms`() {
        // GIVEN
        val nanos = 1_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1ms", formatted)
    }

    @Test
    fun `GIVEN 42 milliseconds WHEN formatting THEN should return 42ms`() {
        // GIVEN
        val nanos = 42_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("42ms", formatted)
    }

    @Test
    fun `GIVEN 150 milliseconds WHEN formatting THEN should return 150ms`() {
        // GIVEN
        val nanos = 150_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("150ms", formatted)
    }

    @Test
    fun `GIVEN 999 milliseconds WHEN formatting THEN should return 999ms`() {
        // GIVEN
        val nanos = 999_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("999ms", formatted)
    }

    // ════════════════════════════════════════
    // Seconds Range (1-59s)
    // ════════════════════════════════════════

    @Test
    fun `GIVEN 1 second WHEN formatting THEN should return 1_0s`() {
        // GIVEN
        val nanos = 1_000_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1.0s", formatted)
    }

    @Test
    fun `GIVEN 1_2 seconds WHEN formatting THEN should return 1_2s`() {
        // GIVEN
        val nanos = 1_238_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1.2s", formatted)
    }

    @Test
    fun `GIVEN 5_8 seconds WHEN formatting THEN should return 5_8s`() {
        // GIVEN
        val nanos = 5_847_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("5.8s", formatted)
    }

    @Test
    fun `GIVEN 59 seconds WHEN formatting THEN should return 59_0s`() {
        // GIVEN
        val nanos = 59_000_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("59.0s", formatted)
    }

    // ════════════════════════════════════════
    // Minutes Range (60s+)
    // ════════════════════════════════════════

    @Test
    fun `GIVEN 1 minute WHEN formatting THEN should return 1m 0s`() {
        // GIVEN
        val nanos = 60_000_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1m 0s", formatted)
    }

    @Test
    fun `GIVEN 1 minute 5 seconds WHEN formatting THEN should return 1m 5s`() {
        // GIVEN
        val nanos = 65_000_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1m 5s", formatted)
    }

    @Test
    fun `GIVEN 2 minutes 30 seconds WHEN formatting THEN should return 2m 30s`() {
        // GIVEN
        val nanos = 150_000_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("2m 30s", formatted)
    }

    @Test
    fun `GIVEN 10 minutes WHEN formatting THEN should return 10m 0s`() {
        // GIVEN
        val nanos = 600_000_000_000L

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("10m 0s", formatted)
    }

    // ════════════════════════════════════════
    // Edge Cases
    // ════════════════════════════════════════

    @Test
    fun `GIVEN threshold between µs and ms WHEN formatting THEN should prefer ms`() {
        // GIVEN
        val nanos = 1_000_000L // Exactly 1ms

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1ms", formatted)
    }

    @Test
    fun `GIVEN threshold between ms and s WHEN formatting THEN should prefer s`() {
        // GIVEN
        val nanos = 1_000_000_000L // Exactly 1s

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1.0s", formatted)
    }

    @Test
    fun `GIVEN threshold between s and m WHEN formatting THEN should prefer m`() {
        // GIVEN
        val nanos = 60_000_000_000L // Exactly 60s = 1m

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1m 0s", formatted)
    }

    // ════════════════════════════════════════
    // Realistic Compiler Timings
    // ════════════════════════════════════════

    @Test
    fun `GIVEN fast analysis timing WHEN formatting THEN should show µs`() {
        // GIVEN
        val nanos = 18_000_000L // 18ms

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("18ms", formatted)
    }

    @Test
    fun `GIVEN normal generation timing WHEN formatting THEN should show ms`() {
        // GIVEN
        val nanos = 45_000_000L // 45ms

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("45ms", formatted)
    }

    @Test
    fun `GIVEN slow interface timing WHEN formatting THEN should show ms`() {
        // GIVEN
        val nanos = 150_000_000L // 150ms

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("150ms", formatted)
    }

    @Test
    fun `GIVEN total compilation timing WHEN formatting THEN should show s`() {
        // GIVEN
        val nanos = 1_238_000_000L // 1238ms = 1.238s

        // WHEN
        val formatted = TimeFormatter.format(nanos)

        // THEN
        assertEquals("1.2s", formatted)
    }
}
