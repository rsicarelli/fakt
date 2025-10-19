// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.propertiesAndMethods
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Comprehensive test for PropertyAndMethodInterface fake generation.
 *
 * Validates:
 * - Property configuration and access
 * - Method behavior configuration
 * - Parameter handling
 * - Default behaviors when not configured
 */
class PropertyAndMethodInterfaceTest {
    @Test
    fun `GIVEN PropertyAndMethodInterface fake WHEN configuring stringValue property THEN should return configured value`() {
        // Given
        val fake =
            fakePropertyAndMethodInterface {
                stringValue { "configured-value" }
            }

        // When
        val result = fake.stringValue

        // Then
        assertEquals("configured-value", result)
    }

    @Test
    fun `GIVEN PropertyAndMethodInterface fake WHEN configuring getValue method THEN should execute behavior`() {
        // Given
        val fake =
            fakePropertyAndMethodInterface {
                getValue { "custom-return-value" }
            }

        // When
        val result = fake.getValue()

        // Then
        assertEquals("custom-return-value", result)
    }

    @Test
    fun `GIVEN PropertyAndMethodInterface fake WHEN configuring setValue method THEN should accept parameter`() {
        // Given
        var capturedValue: String? = null
        val fake =
            fakePropertyAndMethodInterface {
                setValue { value ->
                    capturedValue = value
                }
            }

        // When
        fake.setValue("test-input")

        // Then
        assertEquals("test-input", capturedValue)
    }

    @Test
    fun `GIVEN PropertyAndMethodInterface fake WHEN using defaults THEN should have sensible defaults`() {
        // Given
        val fake = fakePropertyAndMethodInterface()

        // When
        val propertyValue = fake.stringValue
        val methodValue = fake.getValue()
        fake.setValue("ignored") // Should not throw

        // Then
        assertEquals("", propertyValue) // Default for String
        assertEquals("", methodValue) // Default for String
        // setValue should have no-op default behavior
    }
}
