// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.generics

import com.rsicarelli.fakt.samples.singleModule.models.User
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for P1 Scenario: GenericOpenClass
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 *
 * NOTE: These tests document EXPECTED behavior.
 * If compiler doesn't support class-level generics yet, tests will fail/skip.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerTest {
    @Test
    fun `GIVEN generic class WHEN not configured THEN should use super defaults with type safety`() {
        // Given
        val container: Container<String> = fakeContainer {}

        // When
        val value = container.get()
        val items = container.items
        val contains = container.contains("test")

        // Then - uses super (null, empty, false)
        assertNull(value)
        assertTrue(items.isEmpty())
        assertFalse(contains)
    }

    @Test
    fun `GIVEN generic class WHEN configured with String type THEN should work type-safely`() {
        // Given
        val container: Container<String> =
            fakeContainer {
                get { "test-value" }
                set { value -> /* no-op */ }
                items { listOf("item1", "item2") }
                contains { value -> value == "special" }
            }

        // When
        val value = container.get()
        val items = container.items
        val containsSpecial = container.contains("special")
        val containsNormal = container.contains("normal")

        // Then
        assertEquals("test-value", value)
        assertEquals(listOf("item1", "item2"), items)
        assertTrue(containsSpecial)
        assertFalse(containsNormal)
    }

    @Test
    fun `GIVEN generic class WHEN configured with User type THEN should work type-safely`() {
        // Given
        val testUser = User("1", "Test User")
        val container: Container<User> =
            fakeContainer {
                get { testUser }
                items { listOf(testUser) }
            }

        // When
        val value = container.get()
        val items = container.items

        // Then
        assertEquals(testUser, value)
        assertEquals(1, items.size)
        assertEquals(testUser, items[0])
    }

    @Test
    fun `GIVEN generic class WHEN setter configured THEN should capture type-safe values`() {
        // Given
        var capturedValue: String? = null
        val container: Container<String> =
            fakeContainer {
                set { value -> capturedValue = value }
            }

        // When
        container.set("captured")

        // Then
        assertEquals("captured", capturedValue)
    }

    @Test
    fun `GIVEN generic class WHEN multiple instances with different types THEN should maintain type safety`() {
        // Given
        val stringContainer: Container<String> =
            fakeContainer {
                get { "string-value" }
            }
        val intContainer: Container<Int> =
            fakeContainer {
                get { 42 }
            }

        // When
        val stringValue = stringContainer.get()
        val intValue = intContainer.get()

        // Then
        assertEquals("string-value", stringValue)
        assertEquals(42, intValue)
    }
}
