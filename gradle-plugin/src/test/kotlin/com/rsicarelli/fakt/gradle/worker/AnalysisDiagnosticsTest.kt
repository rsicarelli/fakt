// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import kotlin.test.assertEquals
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
    fun `GIVEN exit OK WHEN deciding THEN it succeeds`() {
        assertEquals(AnalysisOutcome.SUCCESS, outcome("OK", fatalCount = 0))
    }

    @Test
    fun `GIVEN COMPILATION_ERROR with no fatal diagnostics WHEN deciding THEN it succeeds`() {
        assertEquals(AnalysisOutcome.SUCCESS, outcome("COMPILATION_ERROR", fatalCount = 0))
    }

    @Test
    fun `GIVEN COMPILATION_ERROR with fatal diagnostics WHEN deciding THEN it fails`() {
        assertEquals(AnalysisOutcome.FAILURE, outcome("COMPILATION_ERROR", fatalCount = 1))
    }

    @Test
    fun `GIVEN any other exit name WHEN deciding THEN it fails even with zero fatal diagnostics`() {
        listOf("INTERNAL_ERROR", "EXCEPTION", "SCRIPT_EXECUTION_ERROR", "SOMETHING_NEW").forEach {
            assertEquals(AnalysisOutcome.FAILURE, outcome(it, fatalCount = 0), it)
        }
    }
}
