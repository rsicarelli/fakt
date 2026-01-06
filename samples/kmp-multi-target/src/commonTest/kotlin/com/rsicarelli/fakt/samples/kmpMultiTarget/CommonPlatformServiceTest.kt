// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CommonPlatformService fake generated in commonTest.
 *
 * This test validates that:
 * 1. Fakt generates fakes for commonMain interfaces in commonTest
 * 2. The generated fake can be configured and used
 * 3. Call tracking works correctly
 */
class CommonPlatformServiceTest {
    @Test
    fun `GIVEN CommonPlatformService fake WHEN logging message THEN should track calls`() {
        // Given
        val fake = fakeCommonPlatformService {
            log { _, _ -> /* no-op */ }
        }

        // When
        fake.log("INFO", "Test message")
        fake.log("ERROR", "Error message")

        // Then
        assertEquals(2, fake.logCallCount.value)
    }

    @Test
    fun `GIVEN CommonPlatformService fake WHEN getting platform name THEN should return configured value`() {
        // Given
        val fake = fakeCommonPlatformService {
            getPlatformName { "TestPlatform" }
        }

        // When
        val result = fake.getPlatformName()

        // Then
        assertEquals("TestPlatform", result)
        assertEquals(1, fake.getPlatformNameCallCount.value)
    }

    @Test
    fun `GIVEN CommonPlatformService fake WHEN accessing debug property THEN should return configured value`() {
        // Given
        val fake = fakeCommonPlatformService {
            isDebugEnabled { true }
        }

        // When
        val result = fake.isDebugEnabled

        // Then
        assertTrue(result)
        assertEquals(1, fake.isDebugEnabledCallCount.value)
    }

    @Test
    fun `GIVEN CommonPlatformService fake WHEN using defaults THEN should have sensible defaults`() {
        // Given
        val fake = fakeCommonPlatformService()

        // When
        fake.log("INFO", "message")
        val platformName = fake.getPlatformName()
        val debugEnabled = fake.isDebugEnabled

        // Then
        assertEquals("", platformName) // Default: empty string
        assertFalse(debugEnabled) // Default: false
        assertEquals(1, fake.logCallCount.value)
        assertEquals(1, fake.getPlatformNameCallCount.value)
        assertEquals(1, fake.isDebugEnabledCallCount.value)
    }
}
