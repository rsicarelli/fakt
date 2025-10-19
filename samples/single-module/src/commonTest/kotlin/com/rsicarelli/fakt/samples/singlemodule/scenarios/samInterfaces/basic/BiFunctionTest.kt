// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.basic

import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.basic.BiFunction
import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.basic.fakeBiFunction
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for BiFunction SAM interface.
 */
class BiFunctionTest {
    @Test
    fun `GIVEN SAM with multiple parameters WHEN applying function THEN should compute result`() {
        // Given
        val biFunction =
            fakeBiFunction {
                apply { a, b -> a + b }
            }

        // When
        val sum = biFunction.apply(10, 20)

        // Then
        assertEquals(30, sum, "Should add two integers")
    }
}
