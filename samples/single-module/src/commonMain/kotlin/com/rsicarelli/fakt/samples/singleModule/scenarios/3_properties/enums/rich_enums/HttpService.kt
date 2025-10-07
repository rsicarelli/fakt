// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.enums.rich_enums

import com.rsicarelli.fakt.Fake

/**
 * HTTP service using rich enums with properties and methods.
 *
 * Tests:
 * - Enum with constructor parameters as property types
 * - Enum with constructor parameters as method parameters
 * - Enum with constructor parameters as method return types
 * - Non-nullable rich enum properties
 * - Nullable rich enum properties
 */
@Fake
interface HttpService {
    /**
     * Default status for successful responses.
     */
    val defaultSuccessStatus: HttpStatus

    /**
     * Optional custom error status.
     */
    val customErrorStatus: HttpStatus?

    /**
     * Send a request and return the response status.
     */
    fun sendRequest(url: String): HttpStatus

    /**
     * Check if a status is retriable.
     */
    fun isRetriable(status: HttpStatus): Boolean

    /**
     * Get status by code.
     */
    fun getStatusByCode(code: Int): HttpStatus?

    /**
     * Map status to response message.
     */
    fun formatResponse(
        status: HttpStatus,
        body: String,
    ): String
}
