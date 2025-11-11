// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.operators.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContainerOperationsTest {
    @Test
    fun `GIVEN ContainerOperations fake WHEN using get operator THEN should return value`() {
        // Given
        val storage = mutableMapOf("key1" to "value1", "key2" to "value2")
        val fake = fakeContainerOperations<String, String> {
            get { key ->
                storage[key]
            }
        }

        // When
        val result = fake["key1"] // Operator syntax!

        // Then
        assertEquals("value1", result)
    }

    @Test
    fun `GIVEN ContainerOperations fake WHEN using set operator THEN should store value`() {
        // Given
        val storage = mutableMapOf<String, String>()
        val fake = fakeContainerOperations<String, String> {
            set { key, value ->
                storage[key] = value
            }
        }

        // When
        fake["key1"] = "value1" // Operator syntax!

        // Then
        assertEquals("value1", storage["key1"])
    }

    @Test
    fun `GIVEN ContainerOperations fake WHEN using contains operator THEN should check membership`() {
        // Given
        val storage = setOf("item1", "item2", "item3")
        val fake = fakeContainerOperations<String, String> {
            contains { key ->
                key in storage
            }
        }

        // When
        val hasItem1 = "item1" in fake // Operator syntax!
        val hasItem4 = "item4" in fake // Operator syntax!

        // Then
        assertTrue(hasItem1)
        assertFalse(hasItem4)
    }

    @Test
    fun `GIVEN ContainerOperations fake WHEN using iterator operator THEN should iterate over entries`() {
        // Given
        val storage = mapOf("k1" to "v1", "k2" to "v2", "k3" to "v3")
        val fake = fakeContainerOperations<String, String> {
            iterator {
                storage.entries.map { it.key to it.value }.iterator()
            }
        }

        // When
        val collected = mutableListOf<Pair<String, String>>()
        for (entry in fake) { // Operator syntax (for-each loop)!
            collected.add(entry)
        }

        // Then
        assertEquals(3, collected.size)
        assertTrue(collected.contains("k1" to "v1"))
        assertTrue(collected.contains("k2" to "v2"))
        assertTrue(collected.contains("k3" to "v3"))
    }

    @Test
    fun `GIVEN ContainerOperations fake WHEN combining operators THEN should work together`() {
        // Given
        val storage = mutableMapOf<String, String>()
        val fake = fakeContainerOperations<String, String> {
            get { key -> storage[key] }
            set { key, value -> storage[key] = value }
            contains { key -> key in storage }
        }

        // When
        fake["key1"] = "value1"
        val hasKey = "key1" in fake
        val value = fake["key1"]
        val noKey = "key2" in fake

        // Then
        assertTrue(hasKey)
        assertEquals("value1", value)
        assertFalse(noKey)
    }

    @Test
    fun `GIVEN ContainerOperations fake WHEN using defaults THEN should use default behavior`() {
        // Given
        val fake = fakeContainerOperations<String, String>() // No configuration

        // When
        val result = fake["key"]
        val contains = "key" in fake

        // Then
        assertNull(result) // Default: null for get
        assertFalse(contains) // Default: false for contains
    }
}

class RangeOperationsTest {
    @Test
    fun `GIVEN RangeOperations fake WHEN using rangeTo operator THEN should create range`() {
        // Given
        val fake = fakeRangeOperations {
            rangeTo { receiver, other ->
                CustomRange(receiver.value, other.value)
            }
        }

        // When
        val range = with(fake) { CustomNumber(1).rangeTo(CustomNumber(10)) }

        // Then
        assertEquals(CustomRange(1, 10), range)
        assertEquals(1, range.start)
        assertEquals(10, range.endInclusive)
    }

    @Test
    fun `GIVEN RangeOperations fake WHEN using rangeUntil operator THEN should create exclusive range`() {
        // Given
        val fake = fakeRangeOperations {
            rangeUntil { receiver, other ->
                // Exclusive range: endInclusive is one less than the upper bound
                CustomRange(receiver.value, other.value - 1)
            }
        }

        // When
        val range = with(fake) { CustomNumber(1).rangeUntil(CustomNumber(10)) }

        // Then
        assertEquals(CustomRange(1, 9), range) // Exclusive: 1..<10 means 1..9
        assertEquals(1, range.start)
        assertEquals(9, range.endInclusive) // 9, not 10 (exclusive)
    }
}

class ComparisonOperationsTest {
    @Test
    fun `GIVEN ComparisonOperations fake WHEN using compareTo THEN should enable comparison operators`() {
        // Given
        data class Version(val major: Int, val minor: Int) : Comparable<Version> {
            override fun compareTo(other: Version): Int =
                compareValuesBy(this, other, { it.major }, { it.minor })
        }

        val fake = fakeComparisonOperations<Version> {
            compareTo { other ->
                -1  // Configure fake to always be "less than" the other version
            }
        }

        // When/Then
        val v1 = Version(1, 0)
        val v2 = Version(2, 0)

        // These use compareTo under the hood
        assertTrue(fake.compareTo(v2) < 0)
        // Note: We can't test v1 > v2 syntax directly without more complex setup
    }
}
