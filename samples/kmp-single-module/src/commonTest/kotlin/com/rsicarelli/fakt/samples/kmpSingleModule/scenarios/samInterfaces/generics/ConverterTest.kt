// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics.Converter
import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics.fakeConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for Converter SAM interface.
 */
class ConverterTest {
    @Test
    fun `GIVEN SAM with two generics WHEN configuring THEN should convert types`() {
        // Given
        val converter =
            fakeConverter<String, Int> {
                convert { input -> input.length }
            }

        // When
        val result = converter.convert("hello")

        // Then
        assertEquals(5, result, "Should convert string to int")
        assertIs<Int>(result, "Should return correct type")
    }
}
