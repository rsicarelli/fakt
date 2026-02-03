// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.consumer

import com.rsicarelli.fakt.samples.publisher.api.fakeHttpClient
import com.rsicarelli.fakt.samples.publisher.api.fakeLogger
import com.rsicarelli.fakt.samples.publisher.api.fakeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Tests for AppService using fakes from PUBLISHED artifacts.
 *
 * This test validates that:
 * 1. Fakes can be imported from published Maven artifact
 * 2. Factory functions (fakeXxx) are accessible
 * 3. DSL configuration works correctly
 * 4. Fake behavior executes as configured
 *
 * If these tests compile and pass, the fake publishing scenario is validated.
 */
class AppServiceTest {

    @Test
    fun `GIVEN configured fakes WHEN syncing data THEN saves items correctly`() = runTest {
        // GIVEN - Fakes from published artifact with configured behavior
        val savedItems = mutableListOf<String>()

        val httpClient = fakeHttpClient {
            get { url -> "item1, item2, item3" }
            post { url, body -> "OK" }
        }

        val repository = fakeRepository {
            name { "TestRepo" }
            fetchAll { savedItems.toList() }
            save { item ->
                savedItems.add(item)
                true
            }
        }

        val logMessages = mutableListOf<String>()
        val logger = fakeLogger {
            level { "DEBUG" }
            log { message -> logMessages.add(message) }
            error { message, _ -> logMessages.add("[ERROR] $message") }
        }

        val service = AppService(httpClient, repository, logger)

        // WHEN - Sync data from URL
        val savedCount = service.syncData("https://api.example.com/data")

        // THEN - Items are saved correctly
        assertEquals(3, savedCount)
        assertEquals(listOf("item1", "item2", "item3"), savedItems)
        assertTrue(logMessages.any { it.contains("Starting sync") })
        assertTrue(logMessages.any { it.contains("Sync complete") })
    }

    @Test
    fun `GIVEN repository with items WHEN getting all items THEN returns expected list`() = runTest {
        // GIVEN
        val httpClient = fakeHttpClient {
            get { "" }
            post { _, _ -> "" }
        }

        val repository = fakeRepository {
            name { "ItemsRepo" }
            fetchAll { listOf("a", "b", "c") }
            save { true }
        }

        val logger = fakeLogger {
            level { "INFO" }
            log { }
            error { _, _ -> }
        }

        val service = AppService(httpClient, repository, logger)

        // WHEN
        val items = service.getAllItems()

        // THEN
        assertEquals(listOf("a", "b", "c"), items)
    }

    @Test
    fun `GIVEN http client WHEN posting data THEN returns response`() = runTest {
        // GIVEN
        var capturedUrl: String? = null
        var capturedBody: String? = null

        val httpClient = fakeHttpClient {
            get { "" }
            post { url, body ->
                capturedUrl = url
                capturedBody = body
                """{"status": "created"}"""
            }
        }

        val repository = fakeRepository {
            name { "Repo" }
            fetchAll { emptyList() }
            save { true }
        }

        val logger = fakeLogger {
            level { "INFO" }
            log { }
            error { _, _ -> }
        }

        val service = AppService(httpClient, repository, logger)

        // WHEN
        val response = service.postData("https://api.example.com/create", """{"name": "test"}""")

        // THEN
        assertEquals("https://api.example.com/create", capturedUrl)
        assertEquals("""{"name": "test"}""", capturedBody)
        assertEquals("""{"status": "created"}""", response)
    }
}
