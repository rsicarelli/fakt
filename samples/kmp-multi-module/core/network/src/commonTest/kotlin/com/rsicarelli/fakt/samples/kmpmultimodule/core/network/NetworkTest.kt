// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for HttpClient fake generation and configuration.
 */
class HttpClientTest {

    @Test
    fun `GIVEN HttpClient fake WHEN performing GET request THEN should return response`() =
        runTest {
            // Given
            val mockResponse =
                HttpResponse(
                    statusCode = 200,
                    body = """{"users":[]}""",
                    headers = mapOf("Content-Type" to "application/json"),
                )

            val client =
                fakeHttpClient {
                    get { url, _, _ ->
                        mockResponse
                    }
                }

            // When
            val response = client.get("https://api.example.com/users")

            // Then
            assertEquals(200, response.statusCode)
            assertTrue(response.isSuccessful)
            assertEquals("""{"users":[]}""", response.body)
        }

    @Test
    fun `GIVEN HttpClient fake WHEN performing POST request THEN should send body and return response`() =
        runTest {
            // Given
            var capturedBody = ""
            val mockResponse = HttpResponse(201, """{"id":"123"}""", emptyMap())

            val client =
                fakeHttpClient {
                    post { _, body, _ ->
                        capturedBody = body
                        mockResponse
                    }
                }

            // When
            val response = client.post("https://api.example.com/users", """{"name":"John"}""")

            // Then
            assertEquals(201, response.statusCode)
            assertEquals("""{"name":"John"}""", capturedBody)
        }

    @Test
    fun `GIVEN HttpClient fake WHEN performing DELETE request THEN should return success`() =
        runTest {
            // Given
            var deletedUrl = ""
            val mockResponse = HttpResponse(204, "", emptyMap())

            val client =
                fakeHttpClient {
                    delete { url, _ ->
                        deletedUrl = url
                        mockResponse
                    }
                }

            // When
            val response = client.delete("https://api.example.com/users/123")

            // Then
            assertEquals(204, response.statusCode)
            assertEquals("https://api.example.com/users/123", deletedUrl)
        }

    @Test
    fun `GIVEN HttpClient fake WHEN configuring baseUrl THEN should return configured value`() {
        // Given
        val client =
            fakeHttpClient {
                baseUrl { "https://api.example.com" }
            }

        // When
        val url = client.baseUrl

        // Then
        assertEquals("https://api.example.com", url)
    }

    @Test
    fun `GIVEN HttpClient fake WHEN configuring timeout THEN should return configured value`() {
        // Given
        val client =
            fakeHttpClient {
                timeoutMillis { 5000L }
            }

        // When
        val timeout = client.timeoutMillis

        // Then
        assertEquals(5000L, timeout)
    }

    @Test
    fun `GIVEN HttpResponse WHEN status is 404 THEN should identify as client error`() {
        // Given
        val response = HttpResponse(404, "Not Found", emptyMap())

        // Then
        assertTrue(response.isClientError)
        assertFalse(response.isSuccessful)
        assertFalse(response.isServerError)
    }

    @Test
    fun `GIVEN HttpResponse WHEN status is 500 THEN should identify as server error`() {
        // Given
        val response = HttpResponse(500, "Internal Server Error", emptyMap())

        // Then
        assertTrue(response.isServerError)
        assertFalse(response.isSuccessful)
        assertFalse(response.isClientError)
    }
}

/**
 * Tests for ApiClient fake generation and configuration.
 */

class ApiClientTest {

    @Test
    fun `GIVEN ApiClient fake WHEN performing typed GET THEN should return success result`() =
        runTest {
            // Given
            val mockResult: ApiResult<String> = ApiResult.Success("user data")
            val client =
                fakeApiClient {
                    get { _, _ ->
                        mockResult
                    }
                }

            // When
            val result: ApiResult<String> = client.get("/users/123")

            // Then
            assertTrue(result.isSuccess())
            assertEquals("user data", result.getOrNull())
        }

    @Test
    fun `GIVEN ApiClient fake WHEN performing typed POST THEN should return success result`() =
        runTest {
            // Given
            val mockResult: ApiResult<String> = ApiResult.Success("created")
            val client =
                fakeApiClient {
                    post<Map<String, String>, String> { _, _, _ ->
                        mockResult
                    }
                }

            // When
            val result: ApiResult<String> = client.post("/users", mapOf("name" to "John"))

            // Then
            assertTrue(result.isSuccess())
            assertEquals("created", result.getOrNull())
        }

    @Test
    fun `GIVEN ApiClient fake WHEN request fails THEN should return error result`() =
        runTest {
            // Given
            val mockResult: ApiResult<String> = ApiResult.Error(404, "Not Found")
            val client =
                fakeApiClient {
                    get { _, _ ->
                        mockResult
                    }
                }

            // When
            val result: ApiResult<String> = client.get("/users/999")

            // Then
            assertTrue(result.isError())
            assertEquals(null, result.getOrNull())
        }

    @Test
    fun `GIVEN ApiClient fake WHEN configuring isAuthenticated THEN should return configured value`() {
        // Given
        val client =
            fakeApiClient {
                isAuthenticated { true }
            }

        // When
        val authenticated = client.isAuthenticated

        // Then
        assertTrue(authenticated)
    }

    @Test
    fun `GIVEN ApiClient fake WHEN configuring apiVersion THEN should return configured value`() {
        // Given
        val client =
            fakeApiClient {
                apiVersion { "v2" }
            }

        // When
        val version = client.apiVersion

        // Then
        assertEquals("v2", version)
    }
}

/**
 * Tests for WebSocketClient fake generation and configuration.
 */
class WebSocketClientTest {

    @Test
    fun `GIVEN WebSocketClient fake WHEN connecting THEN should return connection`() =
        runTest {
            // Given
            val mockConnection = fakeWebSocketConnection()
            val client =
                fakeWebSocketClient {
                    connect { _ ->
                        mockConnection
                    }
                }

            // When
            val connection = client.connect("wss://api.example.com/socket")

            // Then
            assertNotNull(connection)
        }

    @Test
    fun `GIVEN WebSocketClient fake WHEN checking isConnected THEN should return configured value`() {
        // Given
        val client =
            fakeWebSocketClient {
                isConnected { true }
            }

        // When
        val connected = client.isConnected

        // Then
        assertTrue(connected)
    }
}

/**
 * Tests for WebSocketConnection fake generation and configuration.
 */
class WebSocketConnectionTest {

    @Test
    fun `GIVEN WebSocketConnection fake WHEN sending message THEN should capture sent message`() =
        runTest {
            // Given
            var sentMessage = ""
            val connection =
                fakeWebSocketConnection {
                    send { message ->
                        sentMessage = message
                    }
                }

            // When
            connection.send("Hello WebSocket")

            // Then
            assertEquals("Hello WebSocket", sentMessage)
        }

    @Test
    fun `GIVEN WebSocketConnection fake WHEN receiving message THEN should return configured message`() =
        runTest {
            // Given
            val connection =
                fakeWebSocketConnection {
                    receive {
                        "Server message"
                    }
                }

            // When
            val message = connection.receive()

            // Then
            assertEquals("Server message", message)
        }

    @Test
    fun `GIVEN WebSocketConnection fake WHEN checking isOpen THEN should return configured value`() {
        // Given
        val connection =
            fakeWebSocketConnection {
                isOpen { true }
            }

        // When
        val open = connection.isOpen

        // Then
        assertTrue(open)
    }
}
