// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.stdlibTypes

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ResultProcessor SAM interface.
 */
class ResultProcessorTest {
    @Test
    fun `GIVEN ResultProcessor SAM WHEN processing input THEN should return result`() {
        // Given
        val processor = fakeResultProcessor<Int> {
            process { input -> Result.success(input * 2) }
        }

        // When
        val result = processor.process(21)

        // Then
        assertEquals(Result.success(42), result)
    }
}
