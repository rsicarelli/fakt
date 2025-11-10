// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.edgeCases

import kotlin.test.Test
import kotlin.test.assertEquals

class VarargsProcessorTest {
    @Test
    fun `GIVEN VarargsProcessor SAM WHEN processing varargs THEN should handle variable arguments`() {
        // Given
        val processor = fakeVarargsProcessor {
            process { items -> listOf(items.joinToString(",")) }
        }

        // When
        val result = processor.process("a", "b", "c")

        // Then
        assertEquals(listOf("a,b,c"), result, "Should process all varargs")
    }

    @Test
    fun `GIVEN VarargsProcessor SAM WHEN processing empty varargs THEN should handle empty`() {
        // Given
        val processor = fakeVarargsProcessor {
            process { items -> listOf(items.joinToString(",")) }
        }

        // When
        val result = processor.process()

        // Then
        assertEquals(listOf(""), result, "Should handle empty varargs")
    }

    @Test
    fun `GIVEN VarargsProcessor SAM WHEN processing single item THEN should work`() {
        // Given
        val processor = fakeVarargsProcessor {
            process { items -> listOf(items.joinToString(",")) }
        }

        // When
        val result = processor.process("single")

        // Then
        assertEquals(listOf("single"), result, "Should handle single vararg")
    }
}
