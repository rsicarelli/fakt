// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * P0.2: Nested generics test.
 *
 * Tests that nested generic types (List<List<T>>, Map<K, List<V>>) work correctly.
 * This is a common pattern in real-world code for batch operations and grouped data.
 */
class DataCacheTest {
    @Test
    fun `GIVEN DataCache with nested generics WHEN generating fake THEN should preserve nested types`() {
        // Given - Interface with nested generics: List<List<T>>, Map<String, List<T>>
        val cache = fakeDataCache<String>()

        // Then - Should be generated successfully
        assertNotNull(cache, "Fake should be generated for DataCache with nested generics")
    }

    @Test
    fun `GIVEN nested list type WHEN storing batches THEN should maintain type safety`() {
        // Given - DataCache with nested List<List<String>>
        val batches = mutableListOf<List<String>>()
        val cache =
            fakeDataCache<String> {
                storeBatch { items ->
                    batches.add(items)
                }
                getAllBatches { batches }
            }

        // When - Store multiple batches
        cache.storeBatch(listOf("a", "b", "c"))
        cache.storeBatch(listOf("d", "e"))
        val allBatches: List<List<String>> = cache.getAllBatches()

        // Then - Type safety preserved for nested lists
        assertEquals(2, allBatches.size)
        assertEquals(3, allBatches[0].size)
        assertEquals(2, allBatches[1].size)
        assertEquals("a", allBatches[0][0])
    }

    @Test
    fun `GIVEN Map with List values WHEN storing groups THEN should maintain type safety`() {
        // Given - DataCache with Map<String, List<T>>
        val groups = mutableMapOf<String, List<Int>>()
        val cache =
            fakeDataCache<Int> {
                storeGroups { data ->
                    groups.putAll(data)
                }
                getGroup { name ->
                    groups[name]
                }
            }

        // When - Store grouped data
        cache.storeGroups(
            mapOf(
                "evens" to listOf(2, 4, 6),
                "odds" to listOf(1, 3, 5),
            ),
        )
        val evens: List<Int>? = cache.getGroup("evens")
        val odds: List<Int>? = cache.getGroup("odds")

        // Then - Type safety preserved for Map with List values
        assertNotNull(evens)
        assertNotNull(odds)
        assertEquals(3, evens.size)
        assertEquals(listOf(2, 4, 6), evens)
        assertEquals(listOf(1, 3, 5), odds)
    }

    @Test
    fun `GIVEN matrix structure WHEN storing nested lists THEN should preserve structure`() {
        // Given - DataCache with matrix-like structure List<List<T>>
        var storedMatrix: List<List<String>> = emptyList()
        val cache =
            fakeDataCache<String> {
                storeNested { data ->
                    storedMatrix = data
                }
                getMatrix { storedMatrix }
            }

        // When - Store matrix data
        val matrix =
            listOf(
                listOf("a1", "a2", "a3"),
                listOf("b1", "b2", "b3"),
                listOf("c1", "c2", "c3"),
            )
        cache.storeNested(matrix)
        val retrieved: List<List<String>> = cache.getMatrix()

        // Then - Matrix structure preserved
        assertEquals(3, retrieved.size)
        assertEquals(3, retrieved[0].size)
        assertEquals("a1", retrieved[0][0])
        assertEquals("b2", retrieved[1][1])
        assertEquals("c3", retrieved[2][2])
    }

    @Test
    fun `GIVEN complex nested type WHEN using with User objects THEN should maintain type safety`() {
        // Given - DataCache with nested User lists
        val userGroups = mutableMapOf<String, List<User>>()
        val cache =
            fakeDataCache<User> {
                storeGroups { groups ->
                    userGroups.putAll(groups)
                }
                getGroup { name ->
                    userGroups[name]
                }
            }

        // When - Store user groups
        val admins =
            listOf(
                User("1", "Admin1", "admin1@example.com"),
                User("2", "Admin2", "admin2@example.com"),
            )
        val users =
            listOf(
                User("3", "User1", "user1@example.com"),
                User("4", "User2", "user2@example.com"),
            )
        cache.storeGroups(
            mapOf(
                "admins" to admins,
                "users" to users,
            ),
        )
        val retrievedAdmins: List<User>? = cache.getGroup("admins")

        // Then - Complex nested types work correctly
        assertNotNull(retrievedAdmins)
        assertEquals(2, retrievedAdmins.size)
        assertEquals("Admin1", retrievedAdmins[0].name)
        assertEquals("admin1@example.com", retrievedAdmins[0].email)
    }

    @Test
    fun `GIVEN default behaviors WHEN not configured THEN should have sensible defaults`() {
        // Given - Cache without explicit configuration
        val cache = fakeDataCache<String>()

        // When - Call methods with default behavior
        val batches = cache.getAllBatches()
        val group = cache.getGroup("unknown")
        val matrix = cache.getMatrix()

        // Then - Should have sensible defaults for nested types
        assertEquals(emptyList(), batches, "Default getAllBatches() should return empty list")
        assertEquals(null, group, "Default getGroup() should return null")
        assertEquals(emptyList(), matrix, "Default getMatrix() should return empty list")
    }
}
