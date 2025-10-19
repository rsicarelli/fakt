// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.generics

import com.rsicarelli.fakt.samples.singlemodule.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for P1 Scenario: MultiParameterGenericClass
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 *
 * Validates multi-parameter generic class fakes with independent type parameters.
 */
class KeyValueCacheTest {
    @Test
    fun `GIVEN cache with String User types WHEN using typed operations THEN should maintain type safety`() {
        // Given
        val cache: KeyValueCache<String, User> =
            fakeKeyValueCache {
                put { key, value -> /* Store: $key -> $value */ }
                get { key -> if (key == "user1") User("user1", "Alice") else null }
                containsKey { key -> key == "user1" }
            }

        // When
        cache.put("user1", User("user1", "Alice"))
        val retrievedUser = cache.get("user1")
        val containsUser1 = cache.containsKey("user1")
        val containsUser2 = cache.containsKey("user2")

        // Then - Type safety: only User objects, String keys
        assertEquals(User("user1", "Alice"), retrievedUser)
        assertTrue(containsUser1)
        assertFalse(containsUser2)
    }

    @Test
    fun `GIVEN unconfigured cache WHEN calling get THEN should return null from super default`() {
        // Given
        val cache: KeyValueCache<Int, String> = fakeKeyValueCache {}

        // When
        val result = cache.get(42)
        val size = cache.size
        val containsKey = cache.containsKey(42)

        // Then - Super defaults: null, 0, false
        assertNull(result)
        assertEquals(0, size)
        assertFalse(containsKey)
    }

    @Test
    fun `GIVEN configured put WHEN adding entries THEN should call custom behavior`() {
        // Given
        var putCalled = false
        var lastKey: String? = null
        var lastValue: Int? = null

        val cache: KeyValueCache<String, Int> =
            fakeKeyValueCache {
                put { key, value ->
                    putCalled = true
                    lastKey = key
                    lastValue = value
                }
            }

        // When
        cache.put("count", 42)

        // Then - Custom behavior executed
        assertTrue(putCalled)
        assertEquals("count", lastKey)
        assertEquals(42, lastValue)
    }

    @Test
    fun `GIVEN configured containsKey and size WHEN checking THEN should use custom logic`() {
        // Given
        val knownKeys = setOf("key1", "key2", "key3")
        val cache: KeyValueCache<String, String> =
            fakeKeyValueCache {
                containsKey { key -> key in knownKeys }
                size { knownKeys.size }
            }

        // When
        val hasKey1 = cache.containsKey("key1")
        val hasKey4 = cache.containsKey("key4")
        val cacheSize = cache.size

        // Then - Custom logic applied
        assertTrue(hasKey1)
        assertFalse(hasKey4)
        assertEquals(3, cacheSize)
    }

    @Test
    fun `GIVEN cache with Int String types WHEN mixed with String User cache THEN should not interfere`() {
        // Given - Two caches with different type parameters
        val intCache: KeyValueCache<Int, String> =
            fakeKeyValueCache {
                get { id -> "Value-$id" }
            }

        val stringCache: KeyValueCache<String, User> =
            fakeKeyValueCache {
                get { key -> User(key, "User-$key") }
            }

        // When
        val intValue = intCache.get(1)
        val userValue = stringCache.get("alice")

        // Then - Each maintains independent type safety
        assertEquals("Value-1", intValue)
        assertEquals(User("alice", "User-alice"), userValue)
        // Compile-time type safety ensures:
        // intCache.get("string") // Would not compile
        // stringCache.get(123)     // Would not compile
    }
}
