// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.edgeCases

import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for P0 Scenario: ClassWithFinalMethods
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigManagerTest {
    @Test
    fun `GIVEN class with final methods WHEN open methods configured THEN should use configured behavior`() {
        // Given
        val manager =
            fakeConfigManager {
                loadConfig { key -> "custom-$key" }
            }

        // When
        val config = manager.loadConfig("test")

        // Then
        assertEquals("custom-test", config)
    }

    @Test
    fun `GIVEN class with final methods WHEN final methods called THEN should use original implementation`() {
        // Given
        val manager =
            fakeConfigManager {
                // Configure open methods
                loadConfig { "configured" }
            }

        // When - final methods always use original implementation
        val version = manager.getVersion()
        val validKey = manager.validateKey("valid-key")
        val invalidKey = manager.validateKey("")

        // Then - final methods work as-is from original class
        assertEquals("1.0.0", version, "getVersion should use original")
        assertTrue(validKey, "validateKey should use original logic")
        assertFalse(invalidKey, "empty key should be invalid")
    }

    @Test
    fun `GIVEN class with final methods WHEN not configured THEN open methods use super, final use original`() {
        // Given
        val manager =
            fakeConfigManager {
                // Not configuring any methods
            }

        // When
        val config = manager.loadConfig("key")
        val version = manager.getVersion()
        val isValid = manager.validateKey("test")

        // Then
        assertEquals("default-key", config, "loadConfig should use super")
        assertEquals("1.0.0", version, "getVersion should use original")
        assertTrue(isValid, "validateKey should use original")
    }

    @Test
    fun `GIVEN class with final methods WHEN mixing open and final THEN should work correctly together`() {
        // Given
        var saveCalled = false
        lateinit var manager: ConfigManager
        manager =
            fakeConfigManager {
                saveConfig { key, value ->
                    // Use validateKey (final) within configured behavior
                    if (manager.validateKey(key)) {
                        saveCalled = true
                    }
                }
            }

        // When
        manager.saveConfig("valid", "value") // Valid key
        val savedValid = saveCalled
        saveCalled = false
        manager.saveConfig("", "value") // Invalid key

        // Then
        assertTrue(savedValid, "valid key should trigger save")
        assertFalse(saveCalled, "invalid key should not trigger save")
    }
}
