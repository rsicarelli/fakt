// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.basic
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Comprehensive test for AnalyticsService fake generation.
 *
 * Note: @Fake(trackCalls = true) is not yet implemented,
 * so we test the basic fake behavior only.
 *
 * Validates:
 * - Void/Unit methods
 * - Behavior configuration
 * - Default no-op behaviors
 */
class AnalyticsServiceTest {
    @Test
    fun `GIVEN AnalyticsService fake WHEN configuring track THEN should execute behavior`() {
        // Given
        var capturedEvent: String? = null
        val fake =
            fakeAnalyticsService {
                track { event ->
                    capturedEvent = event
                }
            }

        // When
        fake.track("user_clicked_button")

        // Then
        assertEquals("user_clicked_button", capturedEvent)
    }

    @Test
    fun `GIVEN AnalyticsService fake WHEN not configured THEN should have default no-op`() {
        // Given
        val fake = fakeAnalyticsService()

        // When - should not throw
        fake.track("some_event")

        // Then - no exception means success (no-op default)
    }
}
