// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.mixed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for P0 Scenario: MixedAbstractAndOpen
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
class DataServiceTest {
    @Test
    fun `GIVEN mixed class WHEN abstract method not configured THEN should throw error`() {
        // Given
        val service =
            fakeDataService {
                // Not configuring abstract methods
            }

        // When/Then - should error on abstract methods
        assertFailsWith<IllegalStateException> {
            service.fetchData()
        }
        assertFailsWith<IllegalStateException> {
            service.validate("test")
        }
    }

    @Test
    fun `GIVEN mixed class WHEN abstract methods configured THEN should use configured behavior`() {
        // Given
        val service =
            fakeDataService {
                fetchData { "test-data" }
                validate { data -> data.isNotEmpty() }
            }

        // When
        val data = service.fetchData()
        val isValid = service.validate("test")

        // Then
        assertEquals("test-data", data)
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN mixed class WHEN open methods not configured THEN should use super implementation`() {
        // Given
        val service =
            fakeDataService {
                fetchData { "data" }
                validate { true }
                // Not configuring open methods
            }

        // When
        val transformed = service.transform("hello")
        // log just prints, no return value to test

        // Then - should use super implementation
        assertEquals("HELLO", transformed, "transform should use super (uppercase)")
    }

    @Test
    fun `GIVEN mixed class WHEN all methods configured THEN should use configured behaviors`() {
        // Given
        var logCalled = false
        val service =
            fakeDataService {
                // Configure abstract methods
                fetchData { "configured-data" }
                validate { data -> data.length > 5 }
                // Configure open methods
                transform { data -> data.lowercase() }
                log { message -> logCalled = true }
            }

        // When
        val data = service.fetchData()
        val validShort = service.validate("abc")
        val validLong = service.validate("abcdef")
        val transformed = service.transform("HELLO")
        service.log("test")

        // Then
        assertEquals("configured-data", data)
        assertTrue(!validShort, "short string should be invalid")
        assertTrue(validLong, "long string should be valid")
        assertEquals("hello", transformed, "transform should use configured (lowercase)")
        assertTrue(logCalled, "log should have been called")
    }

    @Test
    fun `GIVEN mixed class WHEN partially configured THEN should mix defaults correctly`() {
        // Given - configure only abstract methods
        val service =
            fakeDataService {
                fetchData { "data" }
                validate { true }
                // transform and log use super
            }

        // When
        val data = service.fetchData()
        val transformed = service.transform("test")

        // Then
        assertEquals("data", data, "fetchData uses configured")
        assertEquals("TEST", transformed, "transform uses super")
    }
}
