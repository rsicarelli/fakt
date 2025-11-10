// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.integration

import kotlin.test.assertEquals

/**
 * Base class for integration tests.
 *
 * Provides common assertion helpers for validating generated code.
 * Integration tests verify the complete codegen pipeline:
 * Builder → Model → Renderer → Strategy.
 */
abstract class IntegrationTestBase {

    /**
     * Asserts that generated code matches expected output.
     *
     * Automatically trims whitespace and applies consistent formatting.
     *
     * @param description Description of what's being tested
     * @param expected Expected Kotlin code
     * @param actual Actually generated code
     */
    protected fun assertGeneratedCode(
        description: String,
        expected: String,
        actual: String
    ) {
        val normalizedExpected = expected.trimIndent().trim()
        val normalizedActual = actual.trim()

        assertEquals(
            normalizedExpected,
            normalizedActual,
            "Generated code mismatch for: $description"
        )
    }

    /**
     * Asserts that generated code contains expected substring.
     *
     * Useful for partial validations without full golden file comparison.
     *
     * @param description Description of what's being tested
     * @param expectedSubstring Expected substring in generated code
     * @param actual Actually generated code
     */
    protected fun assertContains(
        description: String,
        expectedSubstring: String,
        actual: String
    ) {
        if (!actual.contains(expectedSubstring)) {
            throw AssertionError(
                "Generated code for '$description' does not contain expected substring.\n" +
                "Expected to find: $expectedSubstring\n" +
                "Actual code:\n$actual"
            )
        }
    }
}
