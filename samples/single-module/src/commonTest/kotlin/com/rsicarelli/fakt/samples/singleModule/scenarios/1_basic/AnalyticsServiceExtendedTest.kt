// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.basic
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance

/**
 * Tests for AnalyticsServiceExtended - Interface extending another interface.
 *
 * Covers:
 * - Interface inheritance (extends AnalyticsService)
 * - Multiple methods (track from parent, identify from child)
 * - Unit return type methods
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsServiceExtendedTest {

    @Test
    fun `GIVEN extended analytics service WHEN tracking event THEN should execute configured behavior`() {
        // Given
        var trackedEvent = ""
        val service = fakeAnalyticsServiceExtended {
            track { event -> trackedEvent = event }
        }

        // When
        service.track("user_click")

        // Then
        assertEquals("user_click", trackedEvent, "Should track the event")
    }

    @Test
    fun `GIVEN extended analytics service WHEN identifying user THEN should execute configured behavior`() {
        // Given
        var identifiedUser = ""
        val service = fakeAnalyticsServiceExtended {
            identify { userId -> identifiedUser = userId }
        }

        // When
        service.identify("user-123")

        // Then
        assertEquals("user-123", identifiedUser, "Should identify the user")
    }

    @Test
    fun `GIVEN extended analytics service WHEN using both methods THEN should work correctly`() {
        // Given
        var events = mutableListOf<String>()
        var users = mutableListOf<String>()

        val service = fakeAnalyticsServiceExtended {
            track { event -> events.add(event) }
            identify { userId -> users.add(userId) }
        }

        // When
        service.track("page_view")
        service.identify("alice")
        service.track("button_click")
        service.identify("bob")

        // Then
        assertEquals(listOf("page_view", "button_click"), events, "Should track all events")
        assertEquals(listOf("alice", "bob"), users, "Should identify all users")
    }

    @Test
    fun `GIVEN extended analytics service WHEN not configured THEN should use default behavior`() {
        // Given - no configuration
        val service = fakeAnalyticsServiceExtended()

        // When & Then - should not throw
        service.track("test")
        service.identify("test-user")
    }
}
