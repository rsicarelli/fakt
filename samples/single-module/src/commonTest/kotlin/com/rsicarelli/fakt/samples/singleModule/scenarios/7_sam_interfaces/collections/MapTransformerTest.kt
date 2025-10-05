// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.MapTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.collections.fakeMapTransformer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for MapTransformer SAM interface.
 */
class MapTransformerTest {
    @Test
    fun `GIVEN MapTransformer SAM WHEN transforming map THEN should return map with string values`() {
        // Given
        val transformer =
            fakeMapTransformer<String, Int> {
                transform { map -> map.mapValues { it.value.toString() } }
            }

        // When
        val result = transformer.transform(mapOf("a" to 1, "b" to 2))

        // Then
        assertEquals(mapOf("a" to "1", "b" to "2"), result)
    }
}
