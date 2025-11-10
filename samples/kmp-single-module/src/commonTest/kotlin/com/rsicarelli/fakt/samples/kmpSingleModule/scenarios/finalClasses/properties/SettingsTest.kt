// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTest {
    @Test
    fun `GIVEN class with mutable properties WHEN not configured THEN should use super getter defaults`() {
        // Given
        val settings = fakeSettings {}

        // When
        val theme = settings.theme
        val fontSize = settings.fontSize
        val autoSave = settings.isAutoSaveEnabled

        // Then - uses super getters
        assertEquals("light", theme)
        assertEquals(14, fontSize)
        assertTrue(autoSave)
    }

    @Test
    fun `GIVEN class with mutable properties WHEN getter configured THEN should return configured value`() {
        // Given
        val settings = fakeSettings {
            theme { "dark" }
            fontSize { 16 }
            isAutoSaveEnabled { false }
        }

        // When
        val theme = settings.theme
        val fontSize = settings.fontSize
        val autoSave = settings.isAutoSaveEnabled

        // Then
        assertEquals("dark", theme)
        assertEquals(16, fontSize)
        assertFalse(autoSave)
    }

    @Test
    fun `GIVEN class with mutable properties WHEN setter used THEN should call super setter`() {
        // Given
        val settings = fakeSettings {
            // Not configuring setters, they should call super
        }

        // When
        settings.theme = "dark"
        settings.fontSize = 20
        settings.isAutoSaveEnabled = false

        // Then - super setters update the backing fields
        assertEquals("dark", settings.theme)
        assertEquals(20, settings.fontSize)
        assertFalse(settings.isAutoSaveEnabled)
    }

    @Test
    fun `GIVEN class with mutable properties WHEN setter configured THEN should use configured behavior`() {
        // Given
        var capturedTheme: String? = null
        var capturedFontSize: Int? = null

        val settings = fakeSettings {
            setTheme { value -> capturedTheme = value }
            setFontSize { value -> capturedFontSize = value }
        }

        // When
        settings.theme = "custom"
        settings.fontSize = 18

        // Then
        assertEquals("custom", capturedTheme)
        assertEquals(18, capturedFontSize)
    }

    @Test
    fun `GIVEN class with mutable properties WHEN method modifies them THEN should work correctly`() {
        // Given
        var themeSetCount = 0
        var fontSizeSetCount = 0

        val settings = fakeSettings {
            setTheme { value -> themeSetCount++ }
            setFontSize { value -> fontSizeSetCount++ }
            setIsAutoSaveEnabled { value -> /* no-op */ }
        }

        // When
        settings.resetToDefaults() // Sets all three properties

        // Then - method called setters
        assertEquals(1, themeSetCount)
        assertEquals(1, fontSizeSetCount)
    }
}
