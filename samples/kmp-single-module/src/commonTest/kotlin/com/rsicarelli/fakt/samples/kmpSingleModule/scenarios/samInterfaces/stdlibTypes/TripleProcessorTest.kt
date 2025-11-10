// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.stdlibTypes

import kotlin.test.Test
import kotlin.test.assertEquals

class TripleProcessorTest {
    @Test
    fun `GIVEN TripleProcessor SAM WHEN processing triple THEN should transform all elements`() {
        // Given
        val processor = fakeTripleProcessor<Int, String, Boolean, String, Int, String> {
            process { triple ->
                Triple(
                    triple.second,
                    triple.first * 2,
                    triple.third.toString(),
                )
            }
        }

        // When
        val result = processor.process(Triple(5, "test", true))

        // Then
        assertEquals(Triple("test", 10, "true"), result, "Should transform all triple elements")
    }

    @Test
    fun `GIVEN TripleProcessor SAM WHEN processing with nullable THEN should handle nulls`() {
        // Given
        val processor = fakeTripleProcessor<Int?, String?, Boolean?, String, String, String> {
            process { triple ->
                Triple(
                    triple.first?.toString() ?: "null",
                    triple.second ?: "null",
                    triple.third?.toString() ?: "null",
                )
            }
        }

        // When
        val result = processor.process(Triple(null, "test", null))

        // Then
        assertEquals(Triple("null", "test", "null"), result, "Should handle nullable elements")
    }
}
