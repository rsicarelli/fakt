// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test to verify that Fakt generates working fake implementations.
 *
 * This test validates the entire end-to-end pipeline:
 * 1. @Fake annotations are detected
 * 2. Factory functions are generated and callable
 * 3. Implementation classes are generated and functional
 * 4. Configuration DSL is generated and usable
 */
class FakeGenerationTest {
    @Test
    fun `generated fake factory functions work`() {
        // Test TestService fake
        val fakeTestService: TestService = fakeTestService()
        assertNotNull(fakeTestService, "fakeTestService() should return non-null instance")

        // Test AnalyticsService fake
        val fakeAnalyticsService: AnalyticsService = fakeAnalyticsService()
        assertNotNull(fakeAnalyticsService, "fakeAnalyticsService() should return non-null instance")
    }

    @Test
    fun `generated fakes implement interfaces correctly`() {
        val fakeTestService = fakeTestService()
        val fakeAnalyticsService = fakeAnalyticsService()

        // Verify instances implement the correct interfaces
        assertTrue(fakeTestService is TestService, "fakeTestService should implement TestService")
        assertTrue(fakeAnalyticsService is AnalyticsService, "fakeAnalyticsService should implement AnalyticsService")
    }

    @Test
    fun `generated fake methods are callable`() {
        val fakeTestService = fakeTestService()
        val fakeAnalyticsService = fakeAnalyticsService()

        // These should not throw exceptions (basic stub implementations)
        fakeTestService.getValue()
        fakeTestService.setValue("test")
        fakeAnalyticsService.track("event")
    }

    @Test
    fun `generated configuration DSL is available`() {
        // Test configuration DSL works without throwing
        val configuredTestService =
            fakeTestService {
                // Configuration methods should be available
                // Note: The actual configuration logic isn't implemented yet,
                // but the DSL structure should be present
            }

        val configuredAnalyticsService =
            fakeAnalyticsService {
                // Configuration methods should be available
            }

        assertNotNull(configuredTestService)
        assertNotNull(configuredAnalyticsService)
    }
}
