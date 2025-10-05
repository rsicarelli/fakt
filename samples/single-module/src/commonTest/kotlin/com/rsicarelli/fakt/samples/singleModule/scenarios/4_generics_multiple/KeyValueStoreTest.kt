// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.genericsMultiple

import com.rsicarelli.fakt.samples.singleModule.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P0.1: Multiple type parameters test ✅
 *
 * Tests that interfaces with multiple type parameters (K, V) generate correctly
 * with full type safety preserved. Validates KeyValueStore<K, V> pattern which is
 * common in real-world code (Map, Cache, etc.).
 */
class KeyValueStoreTest {
    @Test
    fun `GIVEN KeyValueStore with multiple type params WHEN generating fake THEN should preserve both K and V`() {
        // Given - Interface with two type parameters: K and V

        // When - Create fake with String keys and Int values
        val store = fakeKeyValueStore<String, Int>()

        // Then - Should be created successfully
        assertNotNull(store, "Fake should be generated for KeyValueStore<K, V>")
    }

    @Test
    fun `GIVEN multiple type params WHEN configuring behaviors THEN should maintain type safety`() {
        // Given - KeyValueStore with String keys and User values
        val users = mutableMapOf<String, User>()
        val store =
            fakeKeyValueStore<String, User> {
                put { key, value ->
                    users[key] = value
                }
                get { key ->
                    users[key]
                }
                getAll { users.toMap() }
                containsKey { key -> key in users }
            }

        // When - Use the store with type-safe operations
        val user = User("1", "Alice", "alice@example.com")
        store.put("user1", user)
        val retrieved = store.get("user1")
        val allUsers = store.getAll()
        val exists = store.containsKey("user1")

        // Then - Type safety preserved throughout
        assertEquals(user, retrieved)
        assertEquals(1, allUsers.size)
        assertEquals(user, allUsers["user1"])
        assertTrue(exists)
    }

    @Test
    fun `GIVEN different type combinations WHEN creating fakes THEN should work for all combinations`() {
        // Test various type parameter combinations

        // String -> Int
        val stringIntStore =
            fakeKeyValueStore<String, Int> {
                put { key, value -> }
                get { _ -> 42 }
            }
        val intValue: Int? = stringIntStore.get("key")
        assertEquals(42, intValue)

        // Int -> String
        val intStringStore =
            fakeKeyValueStore<Int, String> {
                get { _ -> "value" }
            }
        val stringValue: String? = intStringStore.get(1)
        assertEquals("value", stringValue)

        // String -> List<User>
        val stringListStore =
            fakeKeyValueStore<String, List<User>> {
                get { _ -> listOf(User("1", "Alice", "alice@example.com")) }
            }
        val users: List<User>? = stringListStore.get("key")
        assertEquals(1, users?.size)
    }

    @Test
    fun `GIVEN nullable return type WHEN not configured THEN should use default behavior`() {
        // Given - Store without explicit configuration
        val store = fakeKeyValueStore<String, User>()

        // When - Call methods with default behavior
        val retrieved = store.get("unknown")
        val exists = store.containsKey("unknown")

        // Then - Should have sensible defaults for nullable types
        assertNull(retrieved, "Default get() should return null")
        assertFalse(exists, "Default containsKey() should return false")
    }

    @Test
    fun `GIVEN remove operation WHEN removing existing key THEN should return value`() {
        // Given - Store with remove behavior
        val data = mutableMapOf("key1" to User("1", "Alice", "alice@example.com"))
        val store =
            fakeKeyValueStore<String, User> {
                remove(data::remove)
                containsKey { key -> key in data }
            }

        // When - Remove existing key
        val removed = store.remove("key1")
        val stillExists = store.containsKey("key1")

        // Then - Should return the value and remove it
        assertNotNull(removed)
        assertEquals("Alice", removed.name)
        assertFalse(stillExists)
    }

    @Test
    fun `GIVEN getAll operation WHEN retrieving all entries THEN should return complete map`() {
        // Given - Store with multiple entries
        val users =
            mapOf(
                "1" to User("1", "Alice", "alice@example.com"),
                "2" to User("2", "Bob", "bob@example.com"),
                "3" to User("3", "Charlie", "charlie@example.com"),
            )
        val store =
            fakeKeyValueStore<String, User> {
                getAll { users }
            }

        // When - Get all entries
        val allEntries: Map<String, User> = store.getAll()

        // Then - Should return all entries with correct types
        assertEquals(3, allEntries.size)
        assertEquals("Alice", allEntries["1"]?.name)
        assertEquals("Bob", allEntries["2"]?.name)
        assertEquals("Charlie", allEntries["3"]?.name)
    }
}
