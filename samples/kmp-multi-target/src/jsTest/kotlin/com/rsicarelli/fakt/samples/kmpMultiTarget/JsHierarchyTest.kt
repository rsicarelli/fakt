// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests KMP source set hierarchy: jsTest can access fakes from commonMain.
 *
 * Hierarchy: commonMain → jsMain
 *
 * This test validates that:
 * 1. jsTest can use FakeCommonPlatformServiceImpl (from commonMain)
 * 2. jsTest can use FakeJsOnlyServiceImpl (from jsMain)
 * 3. Fakes from "parent" source sets are accessible in "child" tests
 */
class JsHierarchyTest {
    @Test
    fun `GIVEN jsTest WHEN using CommonPlatformService fake THEN should access fake from commonMain`() {
        // Given - Using fake from commonMain (parent source set)
        val commonFake = fakeCommonPlatformService {
            getPlatformName { "JavaScript via Common" }
            log { level, message -> println("[$level] $message") }
        }

        // When
        commonFake.log("INFO", "Test from JS")
        val result = commonFake.getPlatformName()

        // Then
        assertEquals("JavaScript via Common", result)
        assertEquals(1, commonFake.logCallCount.value)
        assertEquals(1, commonFake.getPlatformNameCallCount.value)
    }

    @Test
    fun `GIVEN jsTest WHEN using JsOnlyService fake THEN should access fake from jsMain`() {
        // Given - Using fake from jsMain (own source set)
        val jsFake = fakeJsOnlyService {
            currentUrl { "https://example.com" }
            getElementById { id ->
                when (id) {
                    "header" -> "<h1>Title</h1>"
                    else -> null
                }
            }
        }

        // When
        val url = jsFake.currentUrl
        val element = jsFake.getElementById("header")

        // Then
        assertEquals("https://example.com", url)
        assertEquals("<h1>Title</h1>", element)
        assertEquals(1, jsFake.currentUrlCallCount.value)
        assertEquals(1, jsFake.getElementByIdCallCount.value)
    }

    @Test
    fun `GIVEN jsTest WHEN using both common and js fakes THEN should access both in hierarchy`() {
        // Given - Using fakes from both commonMain and jsMain
        val commonFake = fakeCommonPlatformService {
            getPlatformName { "JS" }
            isDebugEnabled { false }
        }
        val jsFake = fakeJsOnlyService {
            currentUrl { "https://app.example.com/page" }
        }

        // When
        val platform = commonFake.getPlatformName()
        val debug = commonFake.isDebugEnabled
        val url = jsFake.currentUrl

        // Then - Both fakes work correctly
        assertEquals("JS", platform)
        assertEquals(false, debug)
        assertEquals("https://app.example.com/page", url)
        assertEquals(1, commonFake.getPlatformNameCallCount.value)
        assertEquals(1, commonFake.isDebugEnabledCallCount.value)
        assertEquals(1, jsFake.currentUrlCallCount.value)
    }
}
