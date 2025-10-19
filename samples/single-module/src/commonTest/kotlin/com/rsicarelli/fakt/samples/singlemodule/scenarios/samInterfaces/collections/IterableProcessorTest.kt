// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections.IterableProcessor
import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections.fakeIterableProcessor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for IterableProcessor SAM interface.
 */
class IterableProcessorTest {
    @Test
    fun `GIVEN IterableProcessor SAM WHEN processing iterable THEN should return processed iterable`() {
        // Given
        val processor =
            fakeIterableProcessor<Int> {
                process { iterable -> iterable.map { it + 1 }.asIterable() }
            }

        // When
        val result = processor.process(listOf(1, 2, 3))

        // Then
        assertEquals(listOf(2, 3, 4), result.toList())
    }
}
