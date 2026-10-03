// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.compiler.api.LogLevel
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalysisDiagnosticsTest {

    @Test
    fun `GIVEN a located ERROR without the Fakt prefix WHEN classified THEN it is tolerated`() {
        assertEquals(
            DiagnosticVerdict.TOLERATED,
            classify("ERROR", "Unresolved reference 'serializer'.", hasLocation = true),
        )
    }

    @Test
    fun `GIVEN a located ERROR with the Fakt prefix WHEN classified THEN it is fatal`() {
        assertEquals(
            DiagnosticVerdict.FATAL,
            classify("ERROR", "[FAKT] @Fake Foo references unresolved type(s) X", true),
        )
    }

    @Test
    fun `GIVEN an ERROR without a location WHEN classified THEN it is fatal`() {
        assertEquals(
            DiagnosticVerdict.FATAL,
            classify("ERROR", "Unknown language version: 9.9", hasLocation = false),
        )
    }

    @Test
    fun `GIVEN an EXCEPTION severity WHEN classified THEN it is fatal even with a location`() {
        assertEquals(DiagnosticVerdict.FATAL, classify("EXCEPTION", "boom", hasLocation = true))
    }

    @Test
    fun `GIVEN non-error severities WHEN classified THEN they are ignored whatever the message`() {
        listOf("WARNING", "STRONG_WARNING", "INFO", "LOGGING", "OUTPUT").forEach { severity ->
            listOf(true, false).forEach { located ->
                assertEquals(
                    DiagnosticVerdict.IGNORED,
                    classify(severity, "[FAKT] looks scary", located),
                    "$severity located=$located",
                )
            }
        }
    }

    @Test
    fun `GIVEN exit OK WHEN deciding THEN it succeeds whatever the counts`() {
        listOf(0 to 0, 1 to 0, 0 to 2, 3 to 3).forEach { (fatal, tolerated) ->
            assertEquals(
                AnalysisOutcome.SUCCESS,
                outcome("OK", fatal, tolerated),
                "$fatal/$tolerated",
            )
        }
    }

    @Test
    fun `GIVEN COMPILATION_ERROR with only tolerated diagnostics WHEN deciding THEN it succeeds`() {
        assertEquals(AnalysisOutcome.SUCCESS, outcome("COMPILATION_ERROR", 0, 1))
    }

    @Test
    fun `GIVEN COMPILATION_ERROR with no diagnostics at all WHEN deciding THEN it fails closed`() {
        assertEquals(AnalysisOutcome.FAILURE, outcome("COMPILATION_ERROR", 0, 0))
    }

    @Test
    fun `GIVEN COMPILATION_ERROR with fatal diagnostics WHEN deciding THEN it fails`() {
        assertEquals(AnalysisOutcome.FAILURE, outcome("COMPILATION_ERROR", 1, 0))
        assertEquals(AnalysisOutcome.FAILURE, outcome("COMPILATION_ERROR", 1, 4))
    }

    @Test
    fun `GIVEN any other exit name WHEN deciding THEN it fails even with tolerated diagnostics`() {
        listOf("INTERNAL_ERROR", "EXCEPTION", "SCRIPT_EXECUTION_ERROR", "SOMETHING_NEW").forEach {
            assertEquals(AnalysisOutcome.FAILURE, outcome(it, 0, 2), it)
        }
    }

    private val sample =
        listOf(
            RecordedDiagnostic(
                "Unresolved reference 'a'.",
                "/p/Other.kt:3:7",
                DiagnosticVerdict.TOLERATED,
            ),
            RecordedDiagnostic(
                "Unresolved reference 'b'.",
                "/p/Other.kt:9:1",
                DiagnosticVerdict.TOLERATED,
            ),
        )

    @Test
    fun `GIVEN tolerated errors WHEN level is INFO THEN one summary line is logged`() {
        val lines = toleratedLogLines(sample, LogLevel.INFO)

        assertEquals(
            listOf(
                "Fakt: tolerated compiler error(s) outside @Fake code: 2 " +
                    "(set fakt logLevel to DEBUG to list each one)"
            ),
            lines,
        )
    }

    @Test
    fun `GIVEN tolerated errors WHEN level is DEBUG THEN summary and located lines are logged`() {
        val lines = toleratedLogLines(sample, LogLevel.DEBUG)

        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("Fakt: tolerated compiler error(s)"), lines[0])
        assertEquals(
            "Fakt: tolerated compiler error: /p/Other.kt:3:7: Unresolved reference 'a'.",
            lines[1],
        )
        assertTrue(lines.all { "Fakt: tolerated compiler error" in it })
    }

    @Test
    fun `GIVEN tolerated errors WHEN level is below INFO THEN nothing is logged`() {
        assertEquals(emptyList(), toleratedLogLines(sample, LogLevel.QUIET))
    }

    @Test
    fun `GIVEN no tolerated errors WHEN level is DEBUG THEN nothing is logged`() {
        assertEquals(emptyList(), toleratedLogLines(emptyList(), LogLevel.DEBUG))
    }
}
