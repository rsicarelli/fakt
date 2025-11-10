// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.higherOrder

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for TransformChain SAM interface.
 */
class TransformChainTest {
    @Test
    fun `GIVEN TransformChain SAM WHEN applying chain THEN should compose functions`() {
        // Given
        val chain = fakeTransformChain<Int, String, Boolean> {
            chain { input, first, second ->
                val intermediate = first(input)
                second(intermediate)
            }
        }

        // When
        val result = chain.chain(
            input = 42,
            first = { it.toString() },
            second = { it.length > 1 },
        )

        // Then
        assertEquals(true, result, "Should chain transformations")
    }

    @Test
    fun `GIVEN TransformChain SAM WHEN using complex transforms THEN should handle all steps`() {
        // Given
        val chain = fakeTransformChain<String, List<Char>, Int> {
            chain { input, first, second ->
                val chars = first(input)
                second(chars)
            }
        }

        // When
        val result = chain.chain(
            input = "hello",
            first = { it.toList() },
            second = { it.size },
        )

        // Then
        assertEquals(5, result, "Should process through chain")
    }
}
