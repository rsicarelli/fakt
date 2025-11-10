// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.higherOrder

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for SuspendExecutor SAM interface.
 */
class SuspendExecutorTest {
    @Test
    fun `GIVEN SuspendExecutor SAM WHEN executing suspend function THEN should execute`() =
        runTest {
            // Given
            val executor = fakeSuspendExecutor<Int, String> {
                execute { fn, input -> fn(input) }
            }

            // When
            val result = executor.execute({ input -> input.toString() }, 42)

            // Then
            assertEquals("42", result)
        }
}
