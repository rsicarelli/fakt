// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.properties.enums.richEnums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for HttpService fake with rich enum (enum with properties/constructors).
 *
 * Validates that enums with constructor parameters, properties, and methods
 * are properly handled by Fakt code generation.
 */
class HttpServiceTest {
    @Test
    fun `GIVEN HttpService fake WHEN configuring defaultSuccessStatus THEN should return configured status`() {
        // Given
        val httpService =
            fakeHttpService {
                defaultSuccessStatus { HttpStatus.CREATED }
            }

        // When
        val status = httpService.defaultSuccessStatus

        // Then
        assertEquals(HttpStatus.CREATED, status)
        assertEquals(201, status.code)
        assertEquals("Created", status.message)
        assertTrue(status.isSuccess)
    }

    @Test
    fun `GIVEN HttpService fake WHEN configuring customErrorStatus as null THEN should return null`() {
        // Given
        val httpService =
            fakeHttpService {
                customErrorStatus { null }
            }

        // When
        val status = httpService.customErrorStatus

        // Then
        assertNull(status)
    }

    @Test
    fun `GIVEN HttpService fake WHEN configuring customErrorStatus THEN should return configured error status`() {
        // Given
        val httpService =
            fakeHttpService {
                customErrorStatus { HttpStatus.NOT_FOUND }
            }

        // When
        val status = httpService.customErrorStatus

        // Then
        assertEquals(HttpStatus.NOT_FOUND, status)
        assertEquals(404, status?.code)
        assertFalse(status?.isSuccess ?: true)
    }

    @Test
    fun `GIVEN HttpService fake WHEN configuring sendRequest THEN should return configured status`() {
        // Given
        val httpService =
            fakeHttpService {
                sendRequest { url ->
                    when {
                        url.contains("success") -> HttpStatus.OK
                        url.contains("error") -> HttpStatus.INTERNAL_SERVER_ERROR
                        else -> HttpStatus.BAD_REQUEST
                    }
                }
            }

        // When & Then
        assertEquals(HttpStatus.OK, httpService.sendRequest("https://api.example.com/success"))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpService.sendRequest("https://api.example.com/error"))
        assertEquals(HttpStatus.BAD_REQUEST, httpService.sendRequest("https://api.example.com/other"))
    }

    @Test
    fun `GIVEN HttpService fake WHEN configuring isRetriable THEN should check if status is retriable`() {
        // Given
        val httpService =
            fakeHttpService {
                isRetriable { status ->
                    // Server errors and rate limiting are retriable
                    status.isServerError() || status.code == 429
                }
            }

        // When & Then
        assertTrue(httpService.isRetriable(HttpStatus.INTERNAL_SERVER_ERROR))
        assertFalse(httpService.isRetriable(HttpStatus.BAD_REQUEST))
        assertFalse(httpService.isRetriable(HttpStatus.OK))
    }

    @Test
    fun `GIVEN HttpService fake WHEN configuring getStatusByCode THEN should return status by code`() {
        // Given
        val httpService =
            fakeHttpService {
                getStatusByCode { code ->
                    HttpStatus.entries.firstOrNull { it.code == code }
                }
            }

        // When & Then
        assertEquals(HttpStatus.OK, httpService.getStatusByCode(200))
        assertEquals(HttpStatus.NOT_FOUND, httpService.getStatusByCode(404))
        assertNull(httpService.getStatusByCode(999))
    }

    @Test
    fun `GIVEN HttpService fake WHEN configuring formatResponse THEN should format response with status info`() {
        // Given
        val httpService =
            fakeHttpService {
                formatResponse { status, body ->
                    "${status.format()}: $body"
                }
            }

        // When
        val response = httpService.formatResponse(HttpStatus.OK, "Success")

        // Then
        assertEquals("200 OK: Success", response)
    }

    @Test
    fun `GIVEN HttpService fake WHEN using rich enum methods THEN should access enum properties and methods`() {
        // Given
        val httpService =
            fakeHttpService {
                sendRequest { HttpStatus.UNAUTHORIZED }
            }

        // When
        val status = httpService.sendRequest("https://api.example.com")

        // Then
        assertTrue(status.isClientError())
        assertFalse(status.isServerError())
        assertFalse(status.isSuccess)
        assertEquals("401 Unauthorized", status.format())
    }
}
