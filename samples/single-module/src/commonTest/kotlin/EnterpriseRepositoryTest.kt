// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test for EnterpriseRepository fake generation.
 *
 * Validates:
 * - Custom annotation (@CompanyTestDouble) with advanced features
 * - Suspend functions (findData)
 * - Result type for error handling (saveData)
 * - Boolean returns (deleteData)
 */
class EnterpriseRepositoryTest {
    @Test
    fun `GIVEN EnterpriseRepository WHEN calling suspend findData THEN should return filtered list`() =
        runTest {
            // Given
            val fake =
                fakeEnterpriseRepository {
                    findData { query ->
                        val allData = listOf("doc1.txt", "doc2.pdf", "image1.png", "doc3.txt")
                        allData.filter { it.contains(query) }
                    }
                }

            // When
            val txtFiles = fake.findData(".txt")
            val pdfFiles = fake.findData(".pdf")

            // Then
            assertEquals(2, txtFiles.size)
            assertTrue(txtFiles.contains("doc1.txt"))
            assertTrue(txtFiles.contains("doc3.txt"))
            assertEquals(1, pdfFiles.size)
            assertEquals("doc2.pdf", pdfFiles.first())
        }

    @Test
    fun `GIVEN EnterpriseRepository WHEN saving data THEN should return Result with success or failure`() {
        // Given
        val fake =
            fakeEnterpriseRepository {
                saveData { data ->
                    if (data.isNotBlank()) {
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalArgumentException("Data cannot be blank"))
                    }
                }
            }

        // When
        val successResult = fake.saveData("Valid data")
        val failureResult = fake.saveData("")

        // Then
        assertTrue(successResult.isSuccess)
        assertEquals(Unit, successResult.getOrNull())
        assertTrue(failureResult.isFailure)
        assertTrue(failureResult.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `GIVEN EnterpriseRepository WHEN deleting data THEN should return boolean result`() {
        // Given
        val existingIds = mutableSetOf("id1", "id2", "id3")
        val fake =
            fakeEnterpriseRepository {
                deleteData { id ->
                    existingIds.remove(id)
                }
            }

        // When
        val deletedExisting = fake.deleteData("id1")
        val deletedNonExisting = fake.deleteData("id999")

        // Then
        assertTrue(deletedExisting) // id1 existed and was removed
        assertFalse(deletedNonExisting) // id999 didn't exist
        assertEquals(2, existingIds.size) // Only id2 and id3 remain
    }

    @Test
    fun `GIVEN EnterpriseRepository WHEN using defaults THEN should have sensible default values`() =
        runTest {
            // Given
            val fake = fakeEnterpriseRepository()

            // When
            val findResult = fake.findData("any-query")
            val saveResult = fake.saveData("any-data")
            val deleteResult = fake.deleteData("any-id")

            // Then
            assertTrue(findResult.isEmpty()) // Default: empty list
            assertTrue(saveResult.isSuccess) // Default: success with Unit
            assertEquals(Unit, saveResult.getOrNull())
            assertFalse(deleteResult) // Default: false
        }
}
