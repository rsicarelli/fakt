// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.edgeCases

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for P1 Scenario: ClassWithNullableTypes
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
class CacheStorageTest {
    @Test
    fun `GIVEN cache with nullable types WHEN not configured THEN should use super defaults`() {
        // Given
        val cache = fakeCacheStorage {}

        // When
        val value = cache.get("key")
        val removed = cache.remove("key")

        // Then - super returns null
        assertNull(value)
        assertNull(removed)
    }

    @Test
    fun `GIVEN cache WHEN configured with nullable return THEN should handle nulls correctly`() {
        // Given
        val cache =
            fakeCacheStorage {
                get { key ->
                    when (key) {
                        "existing" -> "value"
                        else -> null
                    }
                }
            }

        // When
        val existing = cache.get("existing")
        val missing = cache.get("missing")

        // Then
        assertEquals("value", existing)
        assertNull(missing)
    }

    @Test
    fun `GIVEN cache WHEN configured with nullable parameter THEN should accept null values`() {
        // Given
        var lastValue: String? = "initial"
        val cache =
            fakeCacheStorage {
                put { key, value ->
                    lastValue = value
                }
            }

        // When
        cache.put("key1", "value1")
        val afterNonNull = lastValue
        cache.put("key2", null)
        val afterNull = lastValue

        // Then
        assertEquals("value1", afterNonNull)
        assertNull(afterNull, "should accept null value")
    }

    @Test
    fun `GIVEN cache WHEN find with null prefix THEN should handle null parameter`() {
        // Given
        val cache =
            fakeCacheStorage {
                find { prefix ->
                    when (prefix) {
                        null -> listOf("all", "items")
                        else -> listOf("filtered-$prefix")
                    }
                }
            }

        // When
        val allItems = cache.find(null)
        val filtered = cache.find("test")

        // Then
        assertEquals(2, allItems.size)
        assertTrue(allItems.contains("all"))
        assertEquals(1, filtered.size)
        assertEquals("filtered-test", filtered[0])
    }

    @Test
    fun `GIVEN cache WHEN using super with null THEN should work correctly`() {
        // Given - only configure get, others use super
        val cache =
            fakeCacheStorage {
                get { "configured" }
            }

        // When
        val value = cache.get("any")
        val findNull = cache.find(null) // Super
        val findValue = cache.find("test") // Super

        // Then
        assertEquals("configured", value)
        assertTrue(findNull.isEmpty(), "super returns empty for null")
        assertEquals(listOf("test"), findValue, "super returns list with prefix")
    }
}
