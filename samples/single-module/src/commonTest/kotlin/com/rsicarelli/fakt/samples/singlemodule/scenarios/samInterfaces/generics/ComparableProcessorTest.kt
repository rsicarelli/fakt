// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.generics.ComparableProcessor
import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.generics.fakeComparableProcessor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ComparableProcessor SAM interface.
 */
class ComparableProcessorTest {
    @Test
    fun `GIVEN SAM with generic constraint WHEN using fake THEN should respect bounds`() {
        // Given
        val processor =
            fakeComparableProcessor<String> {
                process { item -> item.uppercase() }
            }

        // When
        val result = processor.process("test")

        // Then
        assertEquals("TEST", result, "Should process comparable type")
    }
}
