// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package foundation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Foundation module tests - Baseline validation
 *
 * Validates:
 * - Fakes generated in same module work correctly
 * - Factory functions accessible (fakeLogger, fakeConfigService, fakeNetworkClient)
 * - Configuration DSL works
 */
class FoundationServicesTest {

    @Test
    fun `GIVEN Logger fake WHEN configuring debug behavior THEN should use configured behavior`() {
        // Given
        var loggedMessage = ""
        var loggedTag = ""
        val logger =
            fakeLogger {
                debug { message, tag ->
                    loggedMessage = message
                    loggedTag = tag
                }
            }

        // When
        logger.debug("Test message", "TestTag")

        // Then
        assertEquals("Test message", loggedMessage)
        assertEquals("TestTag", loggedTag)
    }

    @Test
    fun `GIVEN Logger fake WHEN configuring minLogLevel THEN should use configured level`() {
        // Given
        val logger =
            fakeLogger {
                minLogLevel { LogLevel.ERROR }
            }

        // When
        val level = logger.minLogLevel

        // Then
        assertEquals(LogLevel.ERROR, level)
    }

    @Test
    fun `GIVEN ConfigService fake WHEN configuring getString THEN should return configured value`() {
        // Given
        val configService =
            fakeConfigService {
                getString { key, default ->
                    when (key) {
                        "api.url" -> "https://api.example.com"
                        else -> default
                    }
                }
            }

        // When
        val apiUrl = configService.getString("api.url", "")
        val unknown = configService.getString("unknown", "fallback")

        // Then
        assertEquals("https://api.example.com", apiUrl)
        assertEquals("fallback", unknown)
    }

    @Test
    fun `GIVEN ConfigService fake WHEN configuring environment THEN should return configured environment`() {
        // Given
        val configService =
            fakeConfigService {
                environment { "production" }
            }

        // When
        val env = configService.environment

        // Then
        assertEquals("production", env)
    }

    @Test
    fun `GIVEN NetworkClient fake WHEN configuring suspend get THEN should return configured response`() =
        runTest {
            // Given
            val mockResponse =
                NetworkResponse(
                    statusCode = 200,
                    body = "{\"status\":\"ok\"}",
                    headers = mapOf("Content-Type" to "application/json"),
                )
            val networkClient =
                fakeNetworkClient {
                    get { url, headers ->
                        Result.success(mockResponse)
                    }
                }

            // When
            val result = networkClient.get("https://api.example.com/status")

            // Then
            assertNotNull(result.getOrNull())
            assertEquals(200, result.getOrNull()?.statusCode)
            assertEquals("{\"status\":\"ok\"}", result.getOrNull()?.body)
        }

    @Test
    fun `GIVEN NetworkClient fake WHEN configuring baseUrl THEN should return configured URL`() {
        // Given
        val networkClient =
            fakeNetworkClient {
                baseUrl { "https://api.example.com" }
            }

        // When
        val url = networkClient.baseUrl

        // Then
        assertEquals("https://api.example.com", url)
    }
}
