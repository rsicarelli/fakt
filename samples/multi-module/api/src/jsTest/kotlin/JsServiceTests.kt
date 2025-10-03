// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package api.js

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsServiceTests {
    @Test
    fun `BrowserService fake can be created`() {
        val fakeBrowserService = fakeBrowserService()
        assertNotNull(fakeBrowserService)
        assertTrue(fakeBrowserService is BrowserService)

        // Properties should be accessible
        fakeBrowserService.userAgent
        fakeBrowserService.currentUrl
        fakeBrowserService.localStorage
        fakeBrowserService.sessionStorage
    }

    @Test
    fun `Storage fake can be created`() {
        val fakeStorage = fakeStorage()
        assertNotNull(fakeStorage)
        assertTrue(fakeStorage is Storage)

        // Basic method calls should not throw
        fakeStorage.getItem("key")
        fakeStorage.setItem("key", "value")
        fakeStorage.removeItem("key")
        fakeStorage.clear()
    }

    @Test
    fun `FetchService fake can be created`() {
        val fakeFetchService = fakeFetchService()
        assertNotNull(fakeFetchService)
        assertTrue(fakeFetchService is FetchService)
    }

    @Test
    fun `Response fake can be created`() {
        val fakeResponse = fakeResponse()
        assertNotNull(fakeResponse)
        assertTrue(fakeResponse is Response)

        // Properties should be accessible
        fakeResponse.ok
        fakeResponse.status
        fakeResponse.statusText
    }
}
