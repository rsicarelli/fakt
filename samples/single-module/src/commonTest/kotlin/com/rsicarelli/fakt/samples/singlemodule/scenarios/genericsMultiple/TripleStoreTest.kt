// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.genericsMultiple

import com.rsicarelli.fakt.samples.singlemodule.models.Product
import com.rsicarelli.fakt.samples.singlemodule.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P0.3: Three Type Parameters Test Suite
 *
 * Tests TripleStore<K1, K2, V> to validate:
 * - Correct handling of 3+ type parameters
 * - Type parameter order preservation
 * - Type safety at use-site with complex parameter combinations
 */
class TripleStoreTest {
    @Test
    fun `GIVEN TripleStore with three type parameters WHEN using String-Int-User combination THEN should maintain type safety`() {
        // Given
        val testUser = User("123", "Alice", "alice@example.com")
        val store =
            fakeTripleStore<String, Int, User> {
                get { k1, k2 ->
                    if (k1 == "user" && k2 == 1) testUser else null
                }
                put { k1, k2, value ->
                    if (k1 == "user" && k2 == 1) value else null
                }
            }

        // When
        val retrieved: User? = store.get("user", 1)
        val stored: User? = store.put("user", 1, testUser)

        // Then
        assertEquals(testUser, retrieved, "Should retrieve correct user")
        assertEquals(testUser, stored, "Should return stored user")
    }

    @Test
    fun `GIVEN TripleStore WHEN using different type combinations THEN should preserve parameter order`() {
        // Given - Int, String, Boolean combination
        val store =
            fakeTripleStore<Int, String, Boolean> {
                get { k1, k2 ->
                    k1 > 0 && k2.isNotEmpty()
                }
            }

        // When
        val result: Boolean? = store.get(42, "test")

        // Then
        assertEquals(true, result, "Should compute correct boolean from Int and String keys")
    }

    @Test
    fun `GIVEN TripleStore WHEN using default behaviors THEN should have sensible defaults`() {
        // Given
        val store = fakeTripleStore<String, Int, User>()

        // When - default behaviors
        val getResult: User? = store.get("key1", 1)
        val removeResult: User? = store.remove("key1", 1)
        val containsResult: Boolean = store.contains("key1", 1)

        // Then - verify defaults
        assertNull(getResult, "Default get() should return null")
        assertNull(removeResult, "Default remove() should return null")
        assertFalse(containsResult, "Default contains() should return false")
    }

    @Test
    fun `GIVEN TripleStore WHEN configuring contains behavior THEN should work correctly`() {
        // Given
        val store =
            fakeTripleStore<String, String, Int> {
                contains { k1, k2 -> k1 == "valid" && k2 == "key" }
            }

        // When
        val exists: Boolean = store.contains("valid", "key")
        val notExists: Boolean = store.contains("invalid", "key")

        // Then
        assertTrue(exists, "Should return true for valid keys")
        assertFalse(notExists, "Should return false for invalid keys")
    }

    @Test
    fun `GIVEN TripleStore WHEN partially configured THEN should mix configured and default behaviors`() {
        // Given - Only configure get, leave others as default
        val testProduct = Product(1L, "Widget", 99.99, "Tools")
        val store =
            fakeTripleStore<String, Int, Product> {
                get { k1, k2 ->
                    if (k1 == "product" && k2 == 1) testProduct else null
                }
            }

        // When
        val retrieved: Product? = store.get("product", 1)
        val notFound: Product? = store.get("product", 2)
        val removed: Product? = store.remove("product", 1)
        val contains: Boolean = store.contains("product", 1)

        // Then
        assertEquals(testProduct, retrieved, "Configured get() should work")
        assertNull(notFound, "Configured get() should return null for non-matching keys")
        assertNull(removed, "Default remove() should return null")
        assertFalse(contains, "Default contains() should return false")
    }

    @Test
    fun `GIVEN TripleStore WHEN using complex nested value types THEN should preserve type safety`() {
        // Given - Using List<User> as value type
        val users =
            listOf(
                User("1", "Alice", "alice@example.com"),
                User("2", "Bob", "bob@example.com"),
            )
        val store =
            fakeTripleStore<String, String, List<User>> {
                get { k1, k2 ->
                    if (k1 == "team" && k2 == "dev") users else emptyList()
                }
            }

        // When
        val team: List<User>? = store.get("team", "dev")
        val emptyTeam: List<User>? = store.get("team", "qa")

        // Then
        assertEquals(users, team, "Should return correct user list")
        assertEquals(emptyList(), emptyTeam, "Should return empty list for non-matching keys")
    }
}
