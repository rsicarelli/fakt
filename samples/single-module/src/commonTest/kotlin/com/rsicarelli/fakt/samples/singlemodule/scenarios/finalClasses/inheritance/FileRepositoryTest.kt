// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.inheritance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for P1 Scenario: ClassExtendingAbstractClass
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
class FileRepositoryTest {
    @Test
    fun `GIVEN class extending abstract WHEN abstract methods not configured THEN should throw error`() {
        // Given
        val repository =
            fakeFileRepository {
                // Not configuring inherited abstract methods
            }

        // When/Then - inherited abstract methods require configuration
        assertFailsWith<IllegalStateException> {
            repository.findById("id")
        }
        assertFailsWith<IllegalStateException> {
            repository.save("entity")
        }
    }

    @Test
    fun `GIVEN class extending abstract WHEN abstract methods configured THEN should use configured behavior`() {
        // Given
        val repository =
            fakeFileRepository {
                findById { id -> "found-$id" }
                save { entity -> Unit } // No-op save
            }

        // When
        val result = repository.findById("123")

        // Then
        assertEquals("found-123", result)
    }

    @Test
    fun `GIVEN class extending abstract WHEN own open methods not configured THEN should use super implementation`() {
        // Given
        val repository =
            fakeFileRepository {
                // Configure abstract methods (required)
                findById { null }
                save { }
                // Not configuring own open methods
            }

        // When
        val all = repository.findAll()
        repository.clearCache() // Should use super (no-op)

        // Then - own methods use super defaults
        assertTrue(all.isEmpty(), "findAll should use super (empty list)")
    }

    @Test
    fun `GIVEN class extending abstract WHEN own open methods configured THEN should use configured behavior`() {
        // Given
        val testData = listOf("file1.txt", "file2.txt")
        var cacheClearCalled = false

        val repository =
            fakeFileRepository {
                // Configure abstract (required)
                findById { null }
                save { }
                // Configure own open methods
                findAll { testData }
                clearCache { cacheClearCalled = true }
            }

        // When
        val all = repository.findAll()
        repository.clearCache()

        // Then
        assertEquals(testData, all)
        assertTrue(cacheClearCalled, "clearCache should use configured behavior")
    }

    @Test
    fun `GIVEN class extending abstract WHEN mixing configured and super THEN should work correctly`() {
        // Given
        val repository =
            fakeFileRepository {
                findById { id -> "entity-$id" } // Configure abstract
                save { } // Configure abstract
                findAll { listOf("cached") } // Configure open
                // clearCache uses super (not configured)
            }

        // When
        val found = repository.findById("1")
        val all = repository.findAll()
        repository.clearCache() // Uses super

        // Then
        assertEquals("entity-1", found)
        assertEquals(listOf("cached"), all)
        // clearCache just ran super (no observable effect to test)
    }
}
