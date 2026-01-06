// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for JsOnlyService fake generated in jsTest.
 *
 * This test validates that:
 * 1. Fakt generates fakes for jsMain interfaces in jsTest
 * 2. The fake is ONLY available in JS tests (not in other platforms)
 * 3. JS-specific operations can be mocked
 */
class JsOnlyServiceTest {
    @Test
    fun `GIVEN JsOnlyService fake WHEN getting element by ID THEN should return configured element`() {
        // Given
        val fake = fakeJsOnlyService {
            getElementById { id ->
                when (id) {
                    "header" -> "<h1>Header</h1>"
                    else -> null
                }
            }
        }

        // When
        val header = fake.getElementById("header")
        val missing = fake.getElementById("missing")

        // Then
        assertEquals("<h1>Header</h1>", header)
        assertNull(missing)
        assertEquals(2, fake.getElementByIdCallCount.value)
    }

    @Test
    fun `GIVEN JsOnlyService fake WHEN setting local storage THEN should track calls`() {
        // Given
        val fake = fakeJsOnlyService {
            setLocalStorage { _, _ -> /* no-op */ }
        }

        // When
        fake.setLocalStorage("token", "abc123")
        fake.setLocalStorage("userId", "user-1")

        // Then
        assertEquals(2, fake.setLocalStorageCallCount.value)
    }

    @Test
    fun `GIVEN JsOnlyService fake WHEN getting local storage THEN should return configured value`() {
        // Given
        val storage = mutableMapOf("token" to "abc123", "userId" to "user-1")
        val fake = fakeJsOnlyService {
            getLocalStorage { key -> storage[key] }
        }

        // When
        val token = fake.getLocalStorage("token")
        val userId = fake.getLocalStorage("userId")
        val missing = fake.getLocalStorage("missing")

        // Then
        assertEquals("abc123", token)
        assertEquals("user-1", userId)
        assertNull(missing)
        assertEquals(3, fake.getLocalStorageCallCount.value)
    }

    @Test
    fun `GIVEN JsOnlyService fake WHEN accessing current URL THEN should return configured URL`() {
        // Given
        val fake = fakeJsOnlyService {
            currentUrl { "https://example.com/page" }
        }

        // When
        val result = fake.currentUrl

        // Then
        assertEquals("https://example.com/page", result)
        assertEquals(1, fake.currentUrlCallCount.value)
    }

    @Test
    fun `GIVEN JsOnlyService fake WHEN using defaults THEN should have sensible defaults`() {
        // Given
        val fake = fakeJsOnlyService()

        // When
        val element = fake.getElementById("any")
        fake.setLocalStorage("key", "value")
        val storage = fake.getLocalStorage("any")
        val url = fake.currentUrl

        // Then - Fakt uses identity function { it } for single-param methods
        assertEquals("any", element) // Default: identity returns input ("any")
        assertEquals("any", storage) // Default: identity returns input ("any")
        assertEquals("", url) // Default: empty string (property with no params)
        assertEquals(1, fake.getElementByIdCallCount.value)
        assertEquals(1, fake.setLocalStorageCallCount.value)
        assertEquals(1, fake.getLocalStorageCallCount.value)
        assertEquals(1, fake.currentUrlCallCount.value)
    }
}
