// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.generics_method_level
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for GenericRepository<T> - Complex generic repository with method-level generics.
 *
 * Covers:
 * - Class-level generic type parameter (T)
 * - Method-level generic type parameter (<R>)
 * - Property configuration (items: List<T>)
 * - Mixed generic methods
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericRepositoryTest {
    @Test
    fun `GIVEN generic repository WHEN configured THEN should return configured items`() {
        // Given
        val repo =
            fakeGenericRepository<String> {
                items { listOf("item1", "item2", "item3") }
                findAll { listOf("all1", "all2") }
            }

        // When
        val items = repo.items
        val all = repo.findAll()

        // Then
        assertEquals(listOf("item1", "item2", "item3"), items, "Should return configured items")
        assertEquals(listOf("all1", "all2"), all, "Should return configured findAll")
    }

    @Test
    fun `GIVEN generic repository WHEN finding by ID THEN should return matching item`() {
        // Given
        val repo =
            fakeGenericRepository<String> {
                findById { id -> if (id == "123") "Found Item" else null }
            }

        // When
        val found = repo.findById("123")
        val notFound = repo.findById("999")

        // Then
        assertEquals("Found Item", found, "Should find item with ID 123")
        assertNull(notFound, "Should return null for non-existent ID")
    }

    @Test
    fun `GIVEN generic repository WHEN saving item THEN should return saved item`() {
        // Given
        val repo =
            fakeGenericRepository<String> {
                save { item -> "Saved: $item" }
            }

        // When
        val result = repo.save("test")

        // Then
        assertEquals("Saved: test", result, "Should return saved item")
    }

    @Test
    fun `GIVEN generic repository WHEN saving all THEN should return saved list`() {
        // Given
        val repo =
            fakeGenericRepository<Int> {
                saveAll { items -> items.map { it * 2 } }
            }

        // When
        val result = repo.saveAll(listOf(1, 2, 3))

        // Then
        assertEquals(listOf(2, 4, 6), result, "Should double all items")
    }

    @Test
    fun `GIVEN generic repository WHEN mapping to different type THEN should transform correctly`() {
        // Given
        val repo =
            fakeGenericRepository<String> {
                map<Int> { transformer -> listOf("a", "b").map(transformer) }
            }

        // When
        val lengths: List<Int> = repo.map<Int> { it.length }

        // Then
        assertEquals(listOf(1, 1), lengths, "Should map strings to lengths")
    }

    @Test
    fun `GIVEN generic repository with Int type WHEN using all operations THEN should work type-safely`() {
        // Given
        val repo =
            fakeGenericRepository<Int> {
                items { listOf(1, 2, 3) }
                findAll { listOf(10, 20, 30) }
                findById { id -> id.toIntOrNull() }
                save { item -> item * 10 }
                saveAll { items -> items.sorted() }
                map<Int> { transformer -> listOf(100, 200).map(transformer) }
            }

        // When & Then
        assertEquals(listOf(1, 2, 3), repo.items)
        assertEquals(listOf(10, 20, 30), repo.findAll())
        assertEquals(42, repo.findById("42"))
        assertEquals(50, repo.save(5))
        assertEquals(listOf(1, 2, 3), repo.saveAll(listOf(3, 1, 2)))
        assertEquals(listOf("100", "200"), repo.map<String> { it.toString() })
    }
}
