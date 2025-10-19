// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.higherOrder

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for FunctionComposer SAM interface.
 */
class FunctionComposerTest {
    @Test
    fun `GIVEN FunctionComposer SAM WHEN composing THEN should compose functions`() {
        // Given
        val composer =
            fakeFunctionComposer<Int, String, Int> {
                compose { fn1, fn2, input -> fn2(fn1(input)) }
            }

        // When
        val result = composer.compose({ (it * 2).toString() }, { it.length }, 5)

        // Then
        assertEquals(2, result)
    }
}
