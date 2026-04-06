// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.abstractClass

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Regression test for #70: abstract class with constructor params + generics. */
class DataFormatterTest {
    @Test
    fun `GIVEN abstract class with constructor params WHEN no methods configured THEN should throw error`() {
        // Given
        val formatter = fakeDataFormatter<String> {}

        // When/Then
        assertFailsWith<IllegalStateException> { formatter.format() }
        assertFailsWith<IllegalStateException> { formatter.parse() }
    }

    @Test
    fun `GIVEN abstract class with constructor params WHEN methods configured THEN should use behaviors`() {
        // Given
        val formatter = fakeDataFormatter<String> {
            format { "42.00" }
            parse { "parsed" }
        }

        // When
        val formatted = formatter.format()
        val parsed = formatter.parse()

        // Then
        assertEquals("42.00", formatted)
        assertEquals("parsed", parsed)
    }

    @Test
    fun `GIVEN abstract class with Int type param WHEN configured THEN should return typed value`() {
        // Given
        val formatter = fakeDataFormatter<Int> {
            format { 42 }
            parse { 100 }
        }

        // When/Then
        assertEquals(42, formatter.format())
        assertEquals(100, formatter.parse())
    }
}
