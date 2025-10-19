// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.propertiesAndMethods
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive test for ResultService fake generation.
 *
 * Validates:
 * - Method-level generics with Result<T>
 * - Multiple method-level type parameters (T, R)
 * - Suspend functions with generics
 * - Result type transformations
 */
class ResultServiceTest {
    @Test
    fun `GIVEN ResultService WHEN trying operation THEN should wrap in Result`() {
        // Given
        val fake =
            fakeResultService {
                tryOperation<Any?> { operation ->
                    try {
                        Result.success(operation())
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }

        // When
        val successResult = fake.tryOperation { 42 }
        val failureResult = fake.tryOperation { throw Exception("Error") }

        // Then
        assertTrue(successResult.isSuccess)
        assertEquals(42, successResult.getOrNull())
        assertTrue(failureResult.isFailure)
    }

    @Test
    fun `GIVEN ResultService WHEN mapping result THEN should transform success value`() {
        // Given
        val fake =
            fakeResultService {
                mapResult<Any?, Any?> { result, mapper ->
                    result.map(mapper)
                }
            }

        // When
        val successResult = fake.mapResult(Result.success(5)) { it * 2 }
        val failureResult = fake.mapResult<Int, String>(Result.failure(Exception("Error"))) { it.toString() }

        // Then
        assertTrue(successResult.isSuccess)
        assertEquals(10, successResult.getOrNull())
        assertTrue(failureResult.isFailure)
    }

    @Test
    fun `GIVEN ResultService WHEN trying async operation THEN should handle suspend`() =
        runTest {
            // Given
            val fake =
                fakeResultService {
                    tryAsyncOperation<Any?> { operation ->
                        try {
                            Result.success(operation())
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                }

            // When
            val result = fake.tryAsyncOperation { "async-result" }

            // Then
            assertTrue(result.isSuccess)
            assertEquals("async-result", result.getOrNull())
        }

    @Test
    fun `GIVEN ResultService WHEN combining results THEN should merge list of results`() {
        // Given
        val fake =
            fakeResultService {
                combineResults { results ->
                    val failures = results.filter { it.isFailure }
                    if (failures.isNotEmpty()) {
                        Result.failure(failures.first().exceptionOrNull()!!)
                    } else {
                        Result.success(results.mapNotNull { it.getOrNull() })
                    }
                }
            }

        // When
        val allSuccess =
            fake.combineResults(
                listOf(
                    Result.success("a"),
                    Result.success("b"),
                    Result.success("c"),
                ),
            )
        val hasFailure =
            fake.combineResults(
                listOf(
                    Result.success("a"),
                    Result.failure(Exception("Error")),
                    Result.success("c"),
                ),
            )

        // Then
        assertTrue(allSuccess.isSuccess)
        assertEquals(listOf("a", "b", "c"), allSuccess.getOrNull())
        assertTrue(hasFailure.isFailure)
    }

    @Test
    fun `GIVEN ResultService WHEN using defaults THEN should return success with defaults`() =
        runTest {
            // Given
            val fake = fakeResultService()

            // When - Only test non-generic method
            val combineResult = fake.combineResults(listOf(Result.success("a")))

            // Then
            assertTrue(combineResult.isSuccess)

            // Note: Methods with method-level generics returning Result<T> require configuration:
            // - tryOperation<T>: Cannot safely default Result<T> wrapping
            // - mapResult<T,R>: Cannot safely default generic transformation
            // - tryAsyncOperation<T>: Cannot safely default Result<T> wrapping
        }
}
