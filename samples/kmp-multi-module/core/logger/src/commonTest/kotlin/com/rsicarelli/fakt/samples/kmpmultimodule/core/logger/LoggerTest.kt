// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.logger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LoggerTest {

    @Test
    fun `GIVEN Logger fake WHEN configuring debug THEN should capture message and data`() {
        // Given
        var capturedMessage = ""
        var capturedData: Map<String, String> = emptyMap()

        val logger =
            fakeLogger {
                debug { message, data ->
                    capturedMessage = message
                    capturedData = data
                }
            }

        // When
        logger.debug("User action", mapOf("action" to "click", "button" to "submit"))

        // Then
        assertEquals("User action", capturedMessage)
        assertEquals("click", capturedData["action"])
        assertEquals("submit", capturedData["button"])
    }

    @Test
    fun `GIVEN Logger fake WHEN configuring info THEN should capture message`() {
        // Given
        var capturedMessage = ""
        val logger =
            fakeLogger {
                info { message, _ ->
                    capturedMessage = message
                }
            }

        // When
        logger.info("Service started")

        // Then
        assertEquals("Service started", capturedMessage)
    }

    @Test
    fun `GIVEN Logger fake WHEN configuring warn THEN should capture message and throwable`() {
        // Given
        var capturedMessage = ""
        var capturedThrowable: Throwable? = null

        val logger =
            fakeLogger {
                warn { message, throwable, _ ->
                    capturedMessage = message
                    capturedThrowable = throwable
                }
            }

        val testException = RuntimeException("Test error")

        // When
        logger.warn("Something went wrong", testException)

        // Then
        assertEquals("Something went wrong", capturedMessage)
        assertNotNull(capturedThrowable)
        assertEquals("Test error", capturedThrowable?.message)
    }

    @Test
    fun `GIVEN Logger fake WHEN configuring error THEN should capture error details`() {
        // Given
        var capturedMessage = ""
        var capturedData: Map<String, String> = emptyMap()

        val logger =
            fakeLogger {
                error { message, _, data ->
                    capturedMessage = message
                    capturedData = data
                }
            }

        // When
        logger.error("Failed to process", null, mapOf("orderId" to "12345"))

        // Then
        assertEquals("Failed to process", capturedMessage)
        assertEquals("12345", capturedData["orderId"])
    }

    @Test
    fun `GIVEN Logger fake WHEN configuring minLogLevel THEN should return configured level`() {
        // Given
        val logger =
            fakeLogger {
                minLogLevel { LogLevel.WARN }
            }

        // When
        val level = logger.minLogLevel

        // Then
        assertEquals(LogLevel.WARN, level)
    }
}
