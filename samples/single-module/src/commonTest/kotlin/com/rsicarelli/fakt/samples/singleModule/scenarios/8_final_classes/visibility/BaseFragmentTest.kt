// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.visibility

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for P1 Scenario: ClassWithProtectedMembers
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BaseFragmentTest {

    @Test
    fun `GIVEN class with protected members WHEN not configured THEN should use super implementation`() {
        // Given
        val fragment = fakeBaseFragment {
            // Not configuring protected members
        }

        // When
        fragment.onCreate()
        val initialized = fragment.checkInitialized()

        // Then - uses super implementation
        assertTrue(initialized, "isInitialized should use super (true)")
    }

    @Test
    fun `GIVEN class with protected members WHEN public method configured THEN should work`() {
        // Given
        var onCreateCalled = false
        val fragment = fakeBaseFragment {
            onCreate { onCreateCalled = true }
        }

        // When
        fragment.onCreate()

        // Then
        assertTrue(onCreateCalled, "onCreate should use configured behavior")
    }

    @Test
    fun `GIVEN class with protected property WHEN configured THEN public method sees configured value`() {
        // Given
        val fragment = fakeBaseFragment {
            isInitialized { false } // Override to false
        }

        // When
        val result = fragment.checkInitialized()

        // Then - public method uses configured protected property
        assertFalse(result, "checkInitialized should see configured isInitialized=false")
    }

    @Test
    fun `GIVEN class with protected method WHEN public method calls it THEN configured behavior runs`() {
        // Given
        var initCalled = false
        val fragment = fakeBaseFragment {
            onInit { initCalled = true }
            // onCreate calls onInit internally
        }

        // When
        fragment.onCreate() // This calls onInit internally

        // Then
        assertTrue(initCalled, "onInit should have been called by onCreate")
    }

    @Test
    fun `GIVEN class with protected members WHEN mixing configured and super THEN should work correctly`() {
        // Given
        val fragment = fakeBaseFragment {
            onInit { } // Configure protected method (no-op)
            // onCreate uses super
            // isInitialized uses super
        }

        // When
        fragment.onCreate()
        val initialized = fragment.checkInitialized()

        // Then
        assertTrue(initialized, "isInitialized should still use super")
    }
}
