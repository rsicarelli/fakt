// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections.ListProcessor
import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections.fakeListProcessor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ListProcessor SAM interface.
 */
class ListProcessorTest {
    @Test
    fun `GIVEN ListProcessor SAM WHEN processing THEN should return processed list`() {
        // Given
        val processor =
            fakeListProcessor<Int> {
                process { list -> list.map { it * 2 } }
            }

        // When
        val result = processor.process(listOf(1, 2, 3))

        // Then
        assertEquals(listOf(2, 4, 6), result)
    }
}
