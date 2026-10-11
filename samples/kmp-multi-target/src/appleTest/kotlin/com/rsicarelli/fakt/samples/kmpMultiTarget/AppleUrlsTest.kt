// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSURL

class AppleUrlsTest {
    @Test
    fun `GIVEN AppleUrls fake WHEN resolving a path THEN should return the configured NSURL`() {
        // Given
        val url = NSURL(string = "https://example.com/a")
        val fake = fakeAppleUrls { resolve { url } }

        // When
        val result = fake.resolve("a")

        // Then
        assertEquals("https://example.com/a", result?.absoluteString)
        assertEquals(1, fake.resolveCalls.value.size)
    }

    @Test
    fun `GIVEN AppleUrls fake WHEN fetching THEN should return null by default`() = runTest {
        // Given
        val fake = fakeAppleUrls()

        // When
        val data = fake.fetch(null)

        // Then
        assertNull(data)
        assertEquals(1, fake.fetchCalls.value.size)
    }
}
