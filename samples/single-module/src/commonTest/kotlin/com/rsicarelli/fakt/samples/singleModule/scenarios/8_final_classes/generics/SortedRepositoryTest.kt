// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.generics

import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for P1 Scenario: GenericClassWithConstraint
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 *
 * Validates generic class fakes with type parameter constraints (T : Comparable<T>).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SortedRepositoryTest {
    @Test
    fun `GIVEN repository with Int type WHEN adding numbers THEN should accept comparable types`() {
        // Given
        var addedItems = mutableListOf<Int>()
        val repository: SortedRepository<Int> =
            fakeSortedRepository {
                add { item -> addedItems.add(item) }
                getSorted { addedItems.sorted() }
            }

        // When
        repository.add(5)
        repository.add(1)
        repository.add(3)
        val sorted = repository.getSorted()

        // Then - Type safety: only Int (Comparable<Int>) accepted
        assertEquals(listOf(1, 3, 5), sorted)
        assertEquals(3, addedItems.size)
    }

    @Test
    fun `GIVEN configured findMin WHEN getting min THEN should return custom value`() {
        // Given
        val repository: SortedRepository<Int> =
            fakeSortedRepository {
                findMin { -100 }
            }

        // When
        val min = repository.findMin()

        // Then
        assertEquals(-100, min)
    }

    @Test
    fun `GIVEN configured getSorted WHEN retrieving THEN should return custom sorted list`() {
        // Given
        val repository: SortedRepository<String> =
            fakeSortedRepository {
                getSorted { listOf("alpha", "beta", "gamma", "delta").sorted() }
            }

        // When
        val sorted = repository.getSorted()

        // Then
        assertEquals(listOf("alpha", "beta", "delta", "gamma"), sorted)
        assertTrue(sorted == sorted.sorted()) // Verify it's actually sorted
    }

    @Test
    fun `GIVEN repository with String type WHEN using THEN should work with different comparable type`() {
        // Given
        val repository: SortedRepository<String> =
            fakeSortedRepository {
                findMin { "aaa" }
                findMax { "zzz" }
                getSorted { listOf("middle", "value") }
            }

        // When
        val min = repository.findMin()
        val max = repository.findMax()
        val sorted = repository.getSorted()

        // Then - Different type parameter (String vs Int) works independently
        assertEquals("aaa", min)
        assertEquals("zzz", max)
        assertEquals(listOf("middle", "value"), sorted)
    }

    @Test
    fun `GIVEN unconfigured repository WHEN calling findMax THEN should return null from super default`() {
        // Given
        val repository: SortedRepository<Int> = fakeSortedRepository {}

        // When
        val min = repository.findMin()
        val max = repository.findMax()
        val sorted = repository.getSorted()

        // Then - Super defaults: null, null, emptyList
        assertNull(min)
        assertNull(max)
        assertTrue(sorted.isEmpty())
    }
}
