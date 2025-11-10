// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.higherOrder

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for FunctionExecutor SAM interface.
 */
class FunctionExecutorTest {
    @Test
    fun `GIVEN FunctionExecutor SAM WHEN executing with transform THEN should apply function`() {
        // Given
        val executor = fakeFunctionExecutor<Int, String> {
            execute { fn, input -> fn(input) }
        }

        // When
        val result = executor.execute({ it.toString() }, 42)

        // Then
        assertEquals("42", result, "Should execute transform function")
    }

    @Test
    fun `GIVEN FunctionExecutor SAM WHEN chaining multiple transforms THEN should compose`() {
        // Given
        val executor = fakeFunctionExecutor<String, Int> {
            execute { fn, input ->
                val intermediate = input.uppercase()
                fn(intermediate)
            }
        }

        // When
        val result = executor.execute({ it.length }, "hello")

        // Then
        assertEquals(5, result, "Should chain transformations")
    }
}
