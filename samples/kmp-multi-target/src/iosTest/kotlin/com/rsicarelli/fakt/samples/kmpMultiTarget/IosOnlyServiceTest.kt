// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for IosOnlyService fake generated in iosTest.
 *
 * This test validates that:
 * 1. Fakt generates fakes for iosMain interfaces in iosTest
 * 2. The fake is ONLY available in iOS tests (not in other platforms)
 * 3. iOS-specific operations can be mocked
 */
class IosOnlyServiceTest {
    @Test
    fun `GIVEN IosOnlyService fake WHEN saving preference THEN should track calls`() {
        // Given
        val fake = fakeIosOnlyService {
            savePreference { _, _ -> /* no-op */ }
        }

        // When
        fake.savePreference("theme", "dark")
        fake.savePreference("language", "en")

        // Then
        assertEquals(2, fake.savePreferenceCallCount)
    }

    @Test
    fun `GIVEN IosOnlyService fake WHEN loading preference THEN should return configured value`() {
        // Given
        val preferences = mutableMapOf("theme" to "dark", "language" to "en")
        val fake = fakeIosOnlyService {
            loadPreference { key -> preferences[key] }
        }

        // When
        val theme = fake.loadPreference("theme")
        val language = fake.loadPreference("language")
        val missing = fake.loadPreference("missing")

        // Then
        assertEquals("dark", theme)
        assertEquals("en", language)
        assertNull(missing)
        assertEquals(3, fake.loadPreferenceCallCount)
    }

    @Test
    fun `GIVEN IosOnlyService fake WHEN getting device model THEN should return configured model`() {
        // Given
        val fake = fakeIosOnlyService {
            getDeviceModel { "iPhone 15 Pro" }
        }

        // When
        val result = fake.getDeviceModel()

        // Then
        assertEquals("iPhone 15 Pro", result)
        assertEquals(1, fake.getDeviceModelCallCount)
    }

    @Test
    fun `GIVEN IosOnlyService fake WHEN checking simulator status THEN should return configured value`() {
        // Given
        val fake = fakeIosOnlyService {
            isSimulator { true }
        }

        // When
        val result = fake.isSimulator

        // Then
        assertTrue(result)
        assertEquals(1, fake.isSimulatorCallCount)
    }

    @Test
    fun `GIVEN IosOnlyService fake WHEN using defaults THEN should have sensible defaults`() {
        // Given
        val fake = fakeIosOnlyService()

        // When
        fake.savePreference("key", "value")
        val preference = fake.loadPreference("any")
        val deviceModel = fake.getDeviceModel()
        val simulator = fake.isSimulator

        // Then - Fakt uses identity function { it } for single-param methods
        assertEquals("any", preference) // Default: identity returns input ("any")
        assertEquals("", deviceModel) // Default: empty string (no-param function)
        assertFalse(simulator) // Default: false (property)
        assertEquals(1, fake.savePreferenceCallCount)
        assertEquals(1, fake.loadPreferenceCallCount)
        assertEquals(1, fake.getDeviceModelCallCount)
        assertEquals(1, fake.isSimulatorCallCount)
    }
}
