// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsMultiple

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CacheServiceTest {
    @Test
    fun `GIVEN CacheService WHEN accessing size properties THEN should return configured values`() {
        // Given
        val fake = fakeCacheService<String, Int> {
            size { 5 }
            maxSize { 100 }
        }

        // When
        val currentSize = fake.size
        val max = fake.maxSize

        // Then
        assertEquals(5, currentSize)
        assertEquals(100, max)
    }

    @Test
    fun `GIVEN CacheService WHEN getting and putting THEN should handle key-value operations`() {
        // Given
        val storage = mutableMapOf<String, Int>()
        val fake = fakeCacheService<String, Int> {
            get { key -> storage[key] }
            put { key, value ->
                storage.put(key, value)
            }
        }

        // When
        val initialGet = fake.get("key1")
        fake.put("key1", 42)
        val afterPut = fake.get("key1")

        // Then
        assertNull(initialGet)
        assertEquals(42, afterPut)
    }

    @Test
    fun `GIVEN CacheService WHEN removing THEN should return old value`() {
        // Given
        val storage = mutableMapOf("key1" to 100)
        val fake = fakeCacheService<String, Int> {
            remove { key -> storage.remove(key) }
        }

        // When
        val removed = fake.remove("key1")
        val removedAgain = fake.remove("key1")

        // Then
        assertEquals(100, removed)
        assertNull(removedAgain)
    }

    @Test
    fun `GIVEN CacheService WHEN clearing THEN should execute clear behavior`() {
        // Given
        var clearCalled = false
        val fake = fakeCacheService<String, Int> {
            clear {
                clearCalled = true
            }
        }

        // When
        fake.clear()

        // Then
        assertTrue(clearCalled)
    }

    @Test
    fun `GIVEN CacheService WHEN checking containsKey THEN should return boolean`() {
        // Given
        val storage = setOf("key1", "key2")
        val fake = fakeCacheService<String, Int> {
            containsKey { key -> key in storage }
        }

        // When
        val contains1 = fake.containsKey("key1")
        val contains3 = fake.containsKey("key3")

        // Then
        assertTrue(contains1)
        assertFalse(contains3)
    }

    @Test
    fun `GIVEN CacheService WHEN getting keys and values THEN should return collections`() {
        // Given
        val storage = mapOf("a" to 1, "b" to 2, "c" to 3)
        val fake = fakeCacheService<String, Int> {
            keys { storage.keys }
            values { storage.values }
        }

        // When
        val keys = fake.keys()
        val values = fake.values()

        // Then
        assertEquals(3, keys.size)
        assertTrue(keys.contains("a"))
        assertEquals(3, values.size)
        assertTrue(values.contains(1))
    }

    @Test
    fun `GIVEN CacheService WHEN compute if absent THEN should handle where constraint`() {
        // Given
        val storage = mutableMapOf<String, Int>()
        val fake = fakeCacheService<String, Int> {
            computeIfAbsent { key, computer ->
                storage.getOrPut(key) { computer(key) as Int }
            }
        }

        // When
        val result1 = fake.computeIfAbsent("key1") { 42 }
        val result2 = fake.computeIfAbsent("key1") { 100 } // Should return existing

        // Then
        assertEquals(42, result1)
        assertEquals(42, result2) // Cached value
    }

    @Test
    fun `GIVEN CacheService WHEN async compute if absent THEN should handle suspend with constraint`() =
        runTest {
            // Given
            val storage = mutableMapOf<String, String>()
            val fake = fakeCacheService<String, String> {
                asyncComputeIfAbsent { key, computer ->
                    storage.getOrPut(key) { computer(key) as String }
                }
            }

            // When
            val result = fake.asyncComputeIfAbsent("async-key") { "computed-$it" }

            // Then
            assertEquals("computed-async-key", result)
        }

    @Test
    fun `GIVEN CacheService WHEN using defaults THEN should have sensible defaults`() =
        runTest {
            // Given
            val fake = fakeCacheService<String, Int>()

            // When
            val size = fake.size
            val maxSize = fake.maxSize
            val value = fake.get("any")
            val containsResult = fake.containsKey("any")
            val keys = fake.keys()
            val values = fake.values()

            // Then
            assertEquals(0, size) // Default: 0
            assertNull(maxSize) // Default: null
            assertNull(value) // Default: null
            assertFalse(containsResult) // Default: false
            assertTrue(keys.isEmpty()) // Default: empty set
            assertTrue(values.isEmpty()) // Default: empty collection
        }
}
