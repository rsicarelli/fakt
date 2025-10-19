// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.collections

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for MapWithFunction SAM interface.
 */
class MapWithFunctionTest {
    @Test
    fun `GIVEN MapWithFunction SAM WHEN transforming with mapper THEN should apply function`() {
        // Given
        val mapper =
            fakeMapWithFunction<Int, String> {
                transform { items, fn -> items.map(fn) }
            }

        // When
        val result = mapper.transform(listOf(1, 2, 3)) { (it * 10).toString() }

        // Then
        assertEquals(listOf("10", "20", "30"), result)
    }
}
