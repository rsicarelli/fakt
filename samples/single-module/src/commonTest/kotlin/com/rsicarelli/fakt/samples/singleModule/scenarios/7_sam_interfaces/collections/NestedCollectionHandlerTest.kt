// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.collections.NestedCollectionHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.collections.fakeNestedCollectionHandler
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for NestedCollectionHandler SAM interface.
 */
class NestedCollectionHandlerTest {
    @Test
    fun `GIVEN NestedCollectionHandler SAM WHEN handling nested collections THEN should transform`() {
        // Given
        val handler =
            fakeNestedCollectionHandler {
                handle { data ->
                    data
                        .flatMap { map ->
                            map.entries.map { it.key to it.value.toList() }
                        }.toMap()
                }
            }

        // When
        val input =
            listOf(
                mapOf("a" to setOf(1, 2)),
                mapOf("b" to setOf(3, 4)),
            )
        val result = handler.handle(input)

        // Then
        assertEquals(mapOf("a" to listOf(1, 2), "b" to listOf(3, 4)), result)
    }
}
