// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.sealed

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for NetworkService fake with sealed classes.
 *
 * Validates that sealed classes (enum-like algebraic data types) are
 * properly handled by Fakt code generation, including generic sealed classes.
 */
class NetworkServiceTest {
    @Test
    fun `GIVEN NetworkService fake WHEN configuring currentState THEN should return sealed class state`() {
        // Given
        val networkService =
            fakeNetworkService {
                currentState { NetworkResult.Loading }
            }

        // When
        val state = networkService.currentState

        // Then
        assertIs<NetworkResult.Loading>(state)
    }

    @Test
    fun `GIVEN NetworkService fake WHEN configuring lastResult as null THEN should return null`() {
        // Given
        val networkService =
            fakeNetworkService {
                lastResult { null }
            }

        // When
        val result = networkService.lastResult

        // Then
        assertNull(result)
    }

    @Test
    fun `GIVEN NetworkService fake WHEN configuring lastResult THEN should return sealed class result`() {
        // Given
        val networkService =
            fakeNetworkService {
                lastResult { NetworkResult.Success("cached data") }
            }

        // When
        val result = networkService.lastResult

        // Then
        assertIs<NetworkResult.Success<String>>(result)
        assertEquals("cached data", result.data)
    }

    @Test
    fun `GIVEN NetworkService fake WHEN configuring fetchData THEN should return sealed class from suspend function`() =
        runTest {
            // Given
            val networkService =
                fakeNetworkService {
                    fetchData { url ->
                        delay(10) // Simulate network call
                        when {
                            url.contains("success") -> NetworkResult.Success("data from $url")
                            url.contains("error") -> NetworkResult.Error("Not found", 404)
                            else -> NetworkResult.Loading
                        }
                    }
                }

            // When
            val successResult = networkService.fetchData("https://api.example.com/success")
            val errorResult = networkService.fetchData("https://api.example.com/error")
            val loadingResult = networkService.fetchData("https://api.example.com/other")

            // Then
            assertIs<NetworkResult.Success<String>>(successResult)
            assertEquals("data from https://api.example.com/success", successResult.data)

            assertIs<NetworkResult.Error>(errorResult)
            assertEquals("Not found", errorResult.message)
            assertEquals(404, errorResult.code)

            assertIs<NetworkResult.Loading>(loadingResult)
        }

    @Test
    fun `GIVEN NetworkService fake WHEN configuring handleResult THEN should handle sealed class patterns`() {
        // Given
        val networkService =
            fakeNetworkService {
                handleResult { result ->
                    when (result) {
                        is NetworkResult.Success -> "Success: ${result.data}"
                        is NetworkResult.Error -> "Error: ${result.message} (${result.code})"
                        is NetworkResult.Loading -> "Loading..."
                        is NetworkResult.Idle -> "Idle"
                    }
                }
            }

        // When & Then
        assertEquals(
            "Success: test data",
            networkService.handleResult(NetworkResult.Success("test data")),
        )
        assertEquals(
            "Error: Failed (500)",
            networkService.handleResult(NetworkResult.Error("Failed", 500)),
        )
        assertEquals(
            "Loading...",
            networkService.handleResult(NetworkResult.Loading),
        )
        assertEquals(
            "Idle",
            networkService.handleResult(NetworkResult.Idle),
        )
    }

    @Test
    fun `GIVEN NetworkService fake WHEN configuring mapResult THEN should transform sealed class generic type`() {
        // Given
        // Note: Method-level generics with sealed classes have type erasure limitations
        // This test validates the basic structure, though full generic safety requires runtime checks
        val networkService =
            fakeNetworkService {
                mapResult { result, transform ->
                    @Suppress("UNCHECKED_CAST")
                    when (result) {
                        is NetworkResult.Success<*> -> {
                            val data = result.data
                            NetworkResult.Success(transform(data))
                        }
                        is NetworkResult.Error -> result
                        is NetworkResult.Loading -> NetworkResult.Loading
                        is NetworkResult.Idle -> NetworkResult.Idle
                    }
                }
            }

        // When
        val stringResult = NetworkResult.Success("42")
        val intResult = networkService.mapResult(stringResult) { it.toInt() }

        // Then
        assertIs<NetworkResult.Success<Int>>(intResult)
        assertEquals(42, intResult.data)
    }

    @Test
    fun `GIVEN NetworkService fake WHEN configuring isSuccess THEN should check sealed class type`() {
        // Given
        val networkService =
            fakeNetworkService {
                isSuccess { result ->
                    result is NetworkResult.Success
                }
            }

        // When & Then
        assertTrue(networkService.isSuccess(NetworkResult.Success("data")))
        assertFalse(networkService.isSuccess(NetworkResult.Error("error", 400)))
        assertFalse(networkService.isSuccess(NetworkResult.Loading))
        assertFalse(networkService.isSuccess(NetworkResult.Idle))
    }

    @Test
    fun `GIVEN NetworkService fake WHEN using sealed class data objects THEN should handle singleton cases`() {
        // Given
        val networkService =
            fakeNetworkService {
                currentState { NetworkResult.Idle }
                fetchData { url ->
                    if (url.contains("loading")) NetworkResult.Loading else NetworkResult.Idle
                }
            }

        // When
        val currentState = networkService.currentState

        // Then
        assertIs<NetworkResult.Idle>(currentState)
        assertEquals(NetworkResult.Idle, currentState) // Data objects are singletons
    }

    @Test
    fun `GIVEN NetworkService fake WHEN handling complex sealed hierarchies THEN should preserve type information`() =
        runTest {
            // Given
            val results = mutableListOf<NetworkResult<String>>()
            val networkService =
                fakeNetworkService {
                    fetchData { url ->
                        val result =
                            when {
                                url.endsWith("1") -> NetworkResult.Success("data1")
                                url.endsWith("2") -> NetworkResult.Error("error2", 404)
                                url.endsWith("3") -> NetworkResult.Loading
                                else -> NetworkResult.Idle
                            }
                        results.add(result)
                        result
                    }
                }

            // When
            val result1 = networkService.fetchData("url1")
            val result2 = networkService.fetchData("url2")
            val result3 = networkService.fetchData("url3")
            val result4 = networkService.fetchData("url4")

            // Then
            assertEquals(4, results.size)
            assertIs<NetworkResult.Success<String>>(result1)
            assertIs<NetworkResult.Error>(result2)
            assertIs<NetworkResult.Loading>(result3)
            assertIs<NetworkResult.Idle>(result4)
        }
}
