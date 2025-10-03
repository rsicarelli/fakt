// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package api.shared

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedServiceTests {
    @Test
    fun `NetworkService fake can be created`() {
        val fakeNetworkService = fakeNetworkService()
        assertNotNull(fakeNetworkService)
        assertTrue(fakeNetworkService is NetworkService)
    }

    @Test
    fun `StorageService fake can be created`() {
        val fakeStorageService = fakeStorageService()
        assertNotNull(fakeStorageService)
        assertTrue(fakeStorageService is StorageService)
    }

    @Test
    fun `LoggingService fake can be created with configuration`() {
        val fakeLoggingService =
            fakeLoggingService {
                // Configuration DSL should be available
            }
        assertNotNull(fakeLoggingService)
        assertTrue(fakeLoggingService is LoggingService)

        // Basic method calls should not throw
        fakeLoggingService.debug("test message")
        fakeLoggingService.info("test info")
        fakeLoggingService.warn("test warning")
        fakeLoggingService.error("test error")
    }
}
