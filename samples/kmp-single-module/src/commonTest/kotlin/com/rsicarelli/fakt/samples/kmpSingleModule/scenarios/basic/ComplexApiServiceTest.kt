// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.basic

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComplexApiServiceTest {
    @Test
    fun `GIVEN ComplexApiService WHEN accessing properties THEN should return configured values`() {
        // Given
        val fake = fakeComplexApiService {
            baseUrl { "https://api.example.com" }
            timeout { 5000L }
            retryCount { 3 }
        }

        // When
        val url = fake.baseUrl
        val time = fake.timeout
        val retries = fake.retryCount

        // Then
        assertEquals("https://api.example.com", url)
        assertEquals(5000L, time)
        assertEquals(3, retries)
    }

    @Test
    fun `GIVEN ComplexApiService WHEN making request with defaults THEN should handle all defaults`() {
        // Given
        val fake = fakeComplexApiService {
            makeRequest { endpoint, method, headers, body, timeout ->
                "$method $endpoint (timeout: ${timeout}ms)"
            }
        }

        // When - call with all defaults
        val result1 = fake.makeRequest("/users")
        // When - call with some parameters
        val result2 = fake.makeRequest("/posts", "POST", mapOf("Auth" to "Bearer token"))

        // Then
        assertEquals("GET /users (timeout: 30000ms)", result1)
        assertEquals("POST /posts (timeout: 30000ms)", result2)
    }

    @Test
    fun `GIVEN ComplexApiService WHEN batch requests THEN should handle parallel processing`() =
        runTest {
            // Given
            val fake = fakeComplexApiService {
                makeBatchRequests { requests, parallel, onProgress ->
                    requests.mapIndexed { index, (endpoint, headers) ->
                        onProgress?.invoke(index + 1, requests.size)
                        Result.success("Response from $endpoint")
                    }
                }
            }

            // When
            var progressCalled = 0
            val results = fake.makeBatchRequests(
                listOf(
                    "/users" to emptyMap(),
                    "/posts" to emptyMap(),
                ),
                parallel = true,
            ) { current, total ->
                progressCalled++
            }

            // Then
            assertEquals(2, results.size)
            assertTrue(results.all { it.isSuccess })
            assertEquals(2, progressCalled)
        }

    @Test
    fun `GIVEN ComplexApiService WHEN parsing response THEN should use method-level generic`() {
        // Given
        val fake = fakeComplexApiService {
            parseResponse<Int> { response, parser, fallback ->
                parser(response) ?: fallback
            }
        }

        // When
        val successResult = fake.parseResponse("42", { it.toIntOrNull() })
        val failureResult = fake.parseResponse("invalid", { it.toIntOrNull() })
        val withFallback = fake.parseResponse("invalid", { it.toIntOrNull() }, 0)

        // Then
        assertEquals(42, successResult)
        assertNull(failureResult)
        assertEquals(0, withFallback)
    }

    @Test
    fun `GIVEN ComplexApiService WHEN processing with retry THEN should handle retry logic`() =
        runTest {
            // Given
            val fake = fakeComplexApiService {
                processWithRetry<Any?, Any?> { request, processor, _, _ ->
                    // Simulate successful processing after retries
                    Result.success(processor(request))
                }
            }

            // When
            val result = fake.processWithRetry(
                request = "test-request",
                processor = { req -> "Processed: $req" },
                retryCount = 3,
            ) { _, _ -> /* onRetry callback */ }

            // Then
            assertTrue(result.isSuccess)
            assertEquals("Processed: test-request", result.getOrNull())
        }

    @Test
    fun `GIVEN ComplexApiService WHEN using defaults THEN should have sensible defaults`() =
        runTest {
            // Given
            val fake = fakeComplexApiService()

            // When
            val url = fake.baseUrl
            val timeout = fake.timeout
            val retries = fake.retryCount
            val request = fake.makeRequest("/test")
            val batchResults = fake.makeBatchRequests(emptyList())
            val parsed = fake.parseResponse("test", { it })

            // Then
            assertEquals("", url) // Default: empty string
            assertEquals(0L, timeout) // Default: 0
            assertNull(retries) // Default: null
            assertEquals("", request) // Default: empty string
            assertTrue(batchResults.isEmpty()) // Default: empty list
            assertNull(parsed) // Default: null
        }
}
