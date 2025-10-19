// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.higherOrder

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ResultFunctionMapper SAM interface.
 */
class ResultFunctionMapperTest {
    @Test
    fun `GIVEN ResultFunctionMapper SAM WHEN mapping result function THEN should transform`() {
        // Given
        val mapper =
            fakeResultFunctionMapper<Int, String> {
                mapResult { fn, input -> Result.success(fn(input)) }
            }

        // When
        val result = mapper.mapResult({ it.toString() }, 42)

        // Then
        assertEquals(Result.success("42"), result)
    }
}
