// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.propertiesAndMethods
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Comprehensive test for AsyncDataService fake generation.
 *
 * Validates:
 * - Suspend functions
 * - Method-level generics with suspend
 * - List transformations with suspend
 * - Default async behaviors
 */
class AsyncDataServiceTest {
    @Test
    fun `GIVEN AsyncDataService fake WHEN calling fetchData THEN should return async result`() =
        runTest {
            // Given
            val fake =
                fakeAsyncDataService {
                    fetchData { "async-data-result" }
                }

            // When
            val result = fake.fetchData()

            // Then
            assertEquals("async-data-result", result)
        }

    @Test
    fun `GIVEN AsyncDataService fake WHEN calling processData with generics THEN should preserve type`() =
        runTest {
            // Given
            val fake =
                fakeAsyncDataService {
                    processData<Any?> { data -> data } // Identity function with explicit type
                }

            // When
            val stringResult = fake.processData("test-string")
            val intResult = fake.processData(42)

            // Then
            assertEquals("test-string", stringResult)
            assertEquals(42, intResult)
        }

    @Test
    fun `GIVEN AsyncDataService fake WHEN batch processing THEN should handle list transformation`() =
        runTest {
            // Given
            val fake =
                fakeAsyncDataService {
                    batchProcess { items ->
                        items.map { it.uppercase() }
                    }
                }

            // When
            val result = fake.batchProcess(listOf("a", "b", "c"))

            // Then
            assertEquals(listOf("A", "B", "C"), result)
        }

    @Test
    fun `GIVEN AsyncDataService fake WHEN using defaults THEN should handle suspend defaults`() =
        runTest {
            // Given
            val fake = fakeAsyncDataService()

            // When
            val fetchedData = fake.fetchData()
            val batchResult = fake.batchProcess(listOf("a", "b"))

            // Then
            assertEquals("", fetchedData) // Default: empty string
            assertEquals(listOf("a", "b"), batchResult) // Default: identity for non-generic
            // Note: processData<T> not tested with defaults - generic T requires configuration
        }
}
