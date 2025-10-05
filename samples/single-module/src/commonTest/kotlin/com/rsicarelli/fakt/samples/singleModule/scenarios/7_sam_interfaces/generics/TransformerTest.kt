// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.generics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for Transformer SAM interface.
 */
class TransformerTest {
    @Test
    fun `GIVEN SAM with single generic WHEN using reified fake THEN should be type-safe`() {
        // Given
        val transformer =
            fakeTransformer<String> {
                transform { input -> input.uppercase() }
            }

        // When
        val result = transformer.transform("hello")

        // Then
        assertEquals("HELLO", result, "Should transform string")
        assertIs<String>(result, "Should maintain type safety")
    }

    @Test
    fun `GIVEN generic SAM with identity function WHEN not configured THEN should return default`() {
        // Given - identity transformer without configuration
        val transformer = fakeTransformer<String>()

        // When
        val result = transformer.transform("test")

        // Then
        // Should have identity behavior or return input as-is
        assertEquals("test", result, "Should have identity default behavior")
    }

    @Test
    fun `GIVEN generic SAM with complex type WHEN using custom type THEN should work`() {
        // Given
        data class Person(
            val name: String,
            val age: Int,
        )

        val transformer =
            fakeTransformer<Person> {
                transform { person -> person.copy(age = person.age + 1) }
            }

        // When
        val result = transformer.transform(Person("Alice", 30))

        // Then
        assertEquals(Person("Alice", 31), result, "Should transform custom type")
    }
}
