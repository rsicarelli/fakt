// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic

import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic.StringFormatter
import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic.fakeStringFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class StringFormatterTest {
    @Test
    fun `GIVEN SAM with String return WHEN formatting THEN should convert to string`() {
        // Given
        val formatter = fakeStringFormatter {
            format { value -> "Value: $value" }
        }

        // When
        val result = formatter.format(42)

        // Then
        assertEquals("Value: 42", result, "Should format value as string")
    }
}
