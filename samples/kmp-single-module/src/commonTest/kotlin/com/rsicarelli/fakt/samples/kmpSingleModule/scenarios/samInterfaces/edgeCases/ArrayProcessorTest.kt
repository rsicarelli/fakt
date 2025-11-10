// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.edgeCases

import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayProcessorTest {
    @Test
    fun `GIVEN ArrayProcessor SAM WHEN processing array THEN should return processed array`() {
        // Given
        val processor = fakeArrayProcessor<Int> {
            process { items -> items.map { it * 2 }.toTypedArray() }
        }

        // When
        val result = processor.process(arrayOf(1, 2, 3))

        // Then
        assertEquals(listOf(2, 4, 6), result.toList())
    }
}
