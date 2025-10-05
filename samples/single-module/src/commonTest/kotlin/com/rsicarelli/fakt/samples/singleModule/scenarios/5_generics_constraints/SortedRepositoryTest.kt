// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.genericsConstraints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P1.1: Type Constraint Support Tests
 *
 * Validates that generic type constraints are preserved in generated code.
 * Tests `<T : Comparable<T>>` constraint on SortedRepository<T>.
 */
class SortedRepositoryTest {
    @Test
    fun `GIVEN sorted repository with Int type WHEN configured THEN should maintain type safety with Comparable constraint`() {
        // Given
        val sortedRepo =
            fakeSortedRepository<Int> {
                insert { /* no-op */ }
                findMin { 1 }
                findMax { 100 }
                getAll { listOf(1, 2, 3, 100) }
                sort { listOf(1, 2, 3, 100) }
            }

        // When
        val min: Int? = sortedRepo.findMin()
        val max: Int? = sortedRepo.findMax()
        val all: List<Int> = sortedRepo.getAll()
        val sorted: List<Int> = sortedRepo.sort()

        // Then
        assertEquals(1, min)
        assertEquals(100, max)
        assertEquals(listOf(1, 2, 3, 100), all)
        assertEquals(listOf(1, 2, 3, 100), sorted)
    }

    @Test
    fun `GIVEN sorted repository with String type WHEN configured THEN should maintain type safety with Comparable constraint`() {
        // Given
        val sortedRepo =
            fakeSortedRepository<String> {
                findMin { "Alice" }
                findMax { "Zoe" }
                getAll { listOf("Alice", "Bob", "Zoe") }
                sort { listOf("Alice", "Bob", "Zoe").sorted() }
            }

        // When
        val min: String? = sortedRepo.findMin()
        val max: String? = sortedRepo.findMax()
        val sorted: List<String> = sortedRepo.sort()

        // Then
        assertEquals("Alice", min)
        assertEquals("Zoe", max)
        assertEquals(listOf("Alice", "Bob", "Zoe"), sorted)
    }

    @Test
    fun `GIVEN sorted repository WHEN using default behaviors THEN should have sensible defaults`() {
        // Given
        val sortedRepo = fakeSortedRepository<Int>()

        // When
        val min: Int? = sortedRepo.findMin()
        val max: Int? = sortedRepo.findMax()
        val all: List<Int> = sortedRepo.getAll()
        val sorted: List<Int> = sortedRepo.sort()

        // Then
        assertNull(min)
        assertNull(max)
        assertTrue(all.isEmpty())
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `GIVEN sorted repository with custom comparable type WHEN configured THEN should work with any Comparable type`() {
        // Given - using Double (also Comparable)
        val sortedRepo =
            fakeSortedRepository<Double> {
                findMin { 1.5 }
                findMax { 99.9 }
                getAll { listOf(1.5, 42.0, 99.9) }
                sort { listOf(1.5, 42.0, 99.9) }
            }

        // When
        val min: Double? = sortedRepo.findMin()
        val max: Double? = sortedRepo.findMax()
        val all: List<Double> = sortedRepo.getAll()

        // Then
        assertEquals(1.5, min)
        assertEquals(99.9, max)
        assertEquals(3, all.size)
    }

    @Test
    fun `GIVEN sorted repository WHEN partially configured THEN should use configured and default behaviors`() {
        // Given
        val sortedRepo =
            fakeSortedRepository<Int> {
                findMin { 10 }
                // findMax, getAll, sort use defaults
            }

        // When
        val min: Int? = sortedRepo.findMin()
        val max: Int? = sortedRepo.findMax()
        val all: List<Int> = sortedRepo.getAll()

        // Then
        assertEquals(10, min)
        assertNull(max) // default
        assertTrue(all.isEmpty()) // default
    }

    @Test
    fun `GIVEN sorted repository WHEN insert called THEN should execute configured behavior`() {
        // Given
        var insertedValue: Int? = null
        val sortedRepo =
            fakeSortedRepository<Int> {
                insert { item -> insertedValue = item }
                getAll { listOfNotNull(insertedValue) }
            }

        // When
        sortedRepo.insert(42)
        val all: List<Int> = sortedRepo.getAll()

        // Then
        assertEquals(42, insertedValue)
        assertEquals(listOf(42), all)
    }
}
