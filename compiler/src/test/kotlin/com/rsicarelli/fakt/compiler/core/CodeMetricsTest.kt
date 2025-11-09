// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core

import com.rsicarelli.fakt.compiler.core.telemetry.calculateLOC
import com.rsicarelli.fakt.compiler.core.telemetry.formatBytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

/**
 * Tests for code metrics utility functions.
 *
 * Validates LOC calculation and byte formatting used in telemetry reporting.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeMetricsTest {
    @Test
    fun `GIVEN empty string WHEN calculating LOC THEN should return 0`() {
        // GIVEN
        val code = ""

        // WHEN
        val result = calculateLOC(code)

        // THEN
        assertEquals(0, result)
    }

    @Test
    fun `GIVEN only blank lines WHEN calculating LOC THEN should return 0`() {
        // GIVEN
        val code =
            """



            """.trimIndent()

        // WHEN
        val result = calculateLOC(code)

        // THEN
        assertEquals(0, result)
    }

    @Test
    fun `GIVEN only comments WHEN calculating LOC THEN should return 0`() {
        // GIVEN
        val code =
            """
            // Single line comment
            /* Block comment start
            * Block comment middle
            * Block comment end */
            """.trimIndent()

        // WHEN
        val result = calculateLOC(code)

        // THEN
        assertEquals(0, result)
    }

    @Test
    fun `GIVEN code with mixed content WHEN calculating LOC THEN should count only code lines`() {
        // GIVEN
        val code =
            """
            package com.example

            // This is a comment
            class Foo {
                /* Block comment */
                fun bar() = Unit
            }
            """.trimIndent()

        // WHEN
        val result = calculateLOC(code)

        // THEN
        // Expected lines: "package com.example", "class Foo {", "fun bar() = Unit", "}"
        assertEquals(4, result)
    }

    @Test
    fun `GIVEN bytes less than 1KB WHEN formatting THEN should show in bytes`() {
        // GIVEN
        val bytes = 512L

        // WHEN
        val result = bytes.formatBytes()

        // THEN
        assertEquals("512 B", result)
    }

    @Test
    fun `GIVEN bytes in KB range WHEN formatting THEN should show in kilobytes`() {
        // GIVEN
        val bytes = 2048L // 2 KB

        // WHEN
        val result = bytes.formatBytes()

        // THEN
        assertEquals("2 KB", result)
    }

    @Test
    fun `GIVEN bytes in MB range WHEN formatting THEN should show in megabytes`() {
        // GIVEN
        val bytes = 2_097_152L // 2 MB

        // WHEN
        val result = bytes.formatBytes()

        // THEN
        assertEquals("2 MB", result)
    }
}
