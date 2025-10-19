// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.edgeCases

import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.edgeCases.ArrayProcessor
import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.edgeCases.fakeArrayProcessor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ArrayProcessor SAM interface.
 */
class ArrayProcessorTest {
    @Test
    fun `GIVEN ArrayProcessor SAM WHEN processing array THEN should return processed array`() {
        // Given
        val processor =
            fakeArrayProcessor<Int> {
                process { items -> items.map { it * 2 }.toTypedArray() }
            }

        // When
        val result = processor.process(arrayOf(1, 2, 3))

        // Then
        assertEquals(listOf(2, 4, 6), result.toList())
    }
}
