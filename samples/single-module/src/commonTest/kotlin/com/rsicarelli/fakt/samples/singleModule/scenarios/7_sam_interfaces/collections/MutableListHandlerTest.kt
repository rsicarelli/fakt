// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for MutableListHandler SAM interface.
 */
class MutableListHandlerTest {
    @Test
    fun `GIVEN MutableListHandler SAM WHEN handling THEN should modify list`() {
        // Given
        val handler =
            fakeMutableListHandler<Int> {
                handle { list ->
                    list.add(99)
                    list
                }
            }

        // When
        val list = mutableListOf(1, 2, 3)
        val result = handler.handle(list)

        // Then
        assertEquals(listOf(1, 2, 3, 99), result)
    }
}
