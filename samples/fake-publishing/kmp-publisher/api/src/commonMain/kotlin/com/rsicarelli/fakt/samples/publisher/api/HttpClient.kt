// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.publisher.api

import com.rsicarelli.fakt.Fake

/**
 * HTTP client interface for making network requests.
 * This demonstrates fake generation for interfaces with suspend functions.
 */
@Fake
interface HttpClient {
    /**
     * Performs a GET request to the specified URL.
     *
     * @param url The URL to fetch
     * @return The response body as a string
     */
    suspend fun get(url: String): String

    /**
     * Performs a POST request to the specified URL.
     *
     * @param url The URL to post to
     * @param body The request body
     * @return The response body as a string
     */
    suspend fun post(url: String, body: String): String
}
