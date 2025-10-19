// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.edgeCases

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for RecursiveGeneric SAM interface.
 */
class RecursiveGenericTest {
    @Test
    fun `GIVEN RecursiveGeneric SAM WHEN processing recursive type THEN should handle self-reference`() {
        // Given - create a simple comparable implementation
        data class Node(
            val value: Int,
        ) : Comparable<Node> {
            override fun compareTo(other: Node) = value.compareTo(other.value)
        }

        val processor =
            fakeRecursiveGeneric<Node> {
                process { item -> item.value * 2 }
            }

        // When
        val result = processor.process(Node(21))

        // Then
        assertEquals(42, result, "Should process recursive generic type")
    }

    @Test
    fun `GIVEN RecursiveGeneric SAM WHEN using String THEN should work with comparable`() {
        // Given
        val processor =
            fakeRecursiveGeneric<String> {
                process { item -> item.length }
            }

        // When
        val result = processor.process("hello")

        // Then
        assertEquals(5, result, "Should process String as Comparable")
    }
}
