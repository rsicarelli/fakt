// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.consumer

import com.rsicarelli.fakt.samples.publisher.api.HttpClient
import com.rsicarelli.fakt.samples.publisher.api.Logger
import com.rsicarelli.fakt.samples.publisher.api.Repository

/**
 * Application service that uses the published API interfaces.
 * This demonstrates a consumer application using the library.
 */
class AppService(
    private val httpClient: HttpClient,
    private val repository: Repository,
    private val logger: Logger,
) {
    /**
     * Fetches data from remote API and saves to repository.
     *
     * @param url The URL to fetch data from
     * @return Number of items saved
     */
    suspend fun syncData(url: String): Int {
        logger.log("Starting sync from $url")

        val response = httpClient.get(url)
        val items = response.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        var savedCount = 0
        for (item in items) {
            if (repository.save(item)) {
                savedCount++
            }
        }

        logger.log("Sync complete: saved $savedCount items")
        return savedCount
    }

    /**
     * Gets all items from repository.
     *
     * @return List of all items
     */
    suspend fun getAllItems(): List<String> {
        logger.log("Fetching all items from ${repository.name}")
        return repository.fetchAll()
    }

    /**
     * Posts data to remote API.
     *
     * @param url The URL to post to
     * @param data The data to post
     * @return Response from server
     */
    suspend fun postData(url: String, data: String): String {
        logger.log("Posting data to $url")
        return httpClient.post(url, data)
    }
}
