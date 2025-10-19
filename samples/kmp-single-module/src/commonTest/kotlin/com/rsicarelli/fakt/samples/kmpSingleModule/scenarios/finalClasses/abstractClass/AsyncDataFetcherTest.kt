// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.abstractClass

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for P1 Scenario: AbstractClassWithSuspend
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test + kotlinx-coroutines-test
 */
class AsyncDataFetcherTest {
    @Test
    fun `GIVEN async class WHEN suspend abstract not configured THEN should throw error`() =
        runTest {
            // Given
            val fetcher =
                fakeAsyncDataFetcher {
                    validate { true }
                }

            // When/Then
            assertFailsWith<IllegalStateException> {
                fetcher.fetchData("https://api.test.com")
            }
        }

    @Test
    fun `GIVEN async class WHEN suspend abstract configured THEN should use configured behavior`() =
        runTest {
            // Given
            val fetcher =
                fakeAsyncDataFetcher {
                    fetchData { url -> "data-from-$url" }
                    validate { true }
                }

            // When
            val data = fetcher.fetchData("https://api.test.com")

            // Then
            assertEquals("data-from-https://api.test.com", data)
        }

    @Test
    fun `GIVEN async class WHEN suspend open not configured THEN should use super implementation`() =
        runTest {
            // Given
            val fetcher =
                fakeAsyncDataFetcher {
                    fetchData { "data" }
                    validate { true }
                    // upload not configured
                }

            // When
            val uploadSuccess = fetcher.upload("test-data")
            val uploadFail = fetcher.upload("")

            // Then - should use super implementation
            assertTrue(uploadSuccess, "non-empty data should upload")
            assertFalse(uploadFail, "empty data should not upload")
        }

    @Test
    fun `GIVEN async class WHEN all methods configured THEN should use configured behaviors`() =
        runTest {
            // Given
            var uploadCalled = false
            val fetcher =
                fakeAsyncDataFetcher {
                    fetchData { url -> "configured-$url" }
                    upload { data ->
                        uploadCalled = true
                        true
                    }
                    validate { data -> data.length > 5 }
                }

            // When
            val data = fetcher.fetchData("url")
            val uploaded = fetcher.upload("any")
            val validLong = fetcher.validate("long-string")
            val validShort = fetcher.validate("abc")

            // Then
            assertEquals("configured-url", data)
            assertTrue(uploaded)
            assertTrue(uploadCalled)
            assertTrue(validLong)
            assertFalse(validShort)
        }

    @Test
    fun `GIVEN async class WHEN mixing suspend and regular THEN both should work correctly`() =
        runTest {
            // Given
            val fetcher =
                fakeAsyncDataFetcher {
                    fetchData { "async-data" }
                    validate { data -> data == "valid" }
                }

            // When
            val data = fetcher.fetchData("any") // Suspend
            val isValid = fetcher.validate("valid") // Regular
            val isInvalid = fetcher.validate("invalid")

            // Then
            assertEquals("async-data", data)
            assertTrue(isValid)
            assertFalse(isInvalid)
        }
}
