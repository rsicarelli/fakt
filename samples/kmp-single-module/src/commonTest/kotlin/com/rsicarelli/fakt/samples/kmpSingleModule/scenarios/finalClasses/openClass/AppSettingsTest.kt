// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.openClass

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test
    fun `GIVEN open class with properties WHEN not configured THEN should use super defaults`() {
        // Given
        val settings = fakeAppSettings {}

        // When
        val theme = settings.theme
        val darkMode = settings.isDarkMode
        val retries = settings.maxRetries

        // Then - should use super implementation
        assertEquals("light", theme)
        assertFalse(darkMode)
        assertEquals(3, retries)
    }

    @Test
    fun `GIVEN open class with properties WHEN properties configured THEN should use configured values`() {
        // Given
        val settings = fakeAppSettings {
            theme { "dark" }
            isDarkMode { true }
            maxRetries { 5 }
        }

        // When
        val theme = settings.theme
        val darkMode = settings.isDarkMode
        val retries = settings.maxRetries

        // Then
        assertEquals("dark", theme)
        assertTrue(darkMode)
        assertEquals(5, retries)
    }

    @Test
    fun `GIVEN open class WHEN mixing properties and methods THEN both should work`() {
        // Given
        val settings = fakeAppSettings {
            theme { "custom" }
            getSetting { key -> "value-$key" }
        }

        // When
        val theme = settings.theme
        val setting = settings.getSetting("test")
        val darkMode = settings.isDarkMode // Not configured

        // Then
        assertEquals("custom", theme)
        assertEquals("value-test", setting)
        assertFalse(darkMode, "unconfigured property should use super")
    }

    @Test
    fun `GIVEN open class WHEN partially configured THEN should mix configured and super`() {
        // Given - only configure theme
        val settings = fakeAppSettings {
            theme { "blue" }
        }

        // When
        val theme = settings.theme
        val darkMode = settings.isDarkMode
        val retries = settings.maxRetries
        val setting = settings.getSetting("key")

        // Then
        assertEquals("blue", theme, "configured property")
        assertFalse(darkMode, "super property")
        assertEquals(3, retries, "super property")
        assertNull(setting, "super method")
    }
}
