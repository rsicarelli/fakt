// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.edge_cases

import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * Tests for IntArrayProcessor SAM interface.
 */
class IntArrayProcessorTest {
    @Test
    fun `GIVEN IntArrayProcessor SAM WHEN processing int array THEN should sum elements`() {
        // Given
        val processor =
            fakeIntArrayProcessor {
                process { items -> intArrayOf(items.sum()) }
            }

        // When
        val result = processor.process(intArrayOf(1, 2, 3, 4, 5))

        // Then
        assertContentEquals(intArrayOf(15), result, "Should sum all varargs")
    }

    @Test
    fun `GIVEN IntArrayProcessor SAM WHEN processing empty THEN should return zero`() {
        // Given
        val processor =
            fakeIntArrayProcessor {
                process { items -> intArrayOf(items.sum()) }
            }

        // When
        val result = processor.process(intArrayOf())

        // Then
        assertContentEquals(intArrayOf(0), result, "Should return zero for empty varargs")
    }
}
