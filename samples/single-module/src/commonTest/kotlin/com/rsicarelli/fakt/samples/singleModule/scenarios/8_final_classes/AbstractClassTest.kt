// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance

/**
 * Tests for abstract class fake generation.
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbstractClassTest {

    @Test
    fun `GIVEN abstract class fake WHEN configuring abstract method THEN should use configured behavior`() {
        // Given
        var notificationSent = false
        val fake = fakeNotificationService {
            sendNotification { userId, message ->
                notificationSent = true
            }
        }

        // When
        fake.sendNotification("user-123", "Hello!")

        // Then
        assertEquals(true, notificationSent)
    }

    @Test
    fun `GIVEN abstract class fake WHEN using open method THEN should use super implementation by default`() {
        // Given - fake without configuring formatMessage
        val fake = fakeNotificationService {
            sendNotification { _, _ -> /* no-op */ }
        }

        // When
        val formatted = fake.formatMessage("Test message")

        // Then - should use default implementation from NotificationService
        assertEquals("[NOTIFICATION] Test message", formatted)
    }

    @Test
    fun `GIVEN abstract class fake WHEN overriding open method THEN should use configured behavior`() {
        // Given - override the open method
        val fake = fakeNotificationService {
            sendNotification { _, _ -> /* no-op */ }
            formatMessage { message -> "[CUSTOM] $message" }
        }

        // When
        val formatted = fake.formatMessage("Test")

        // Then
        assertEquals("[CUSTOM] Test", formatted)
    }

    @Test
    fun `GIVEN abstract class fake WHEN calling final method THEN should use original implementation`() {
        // Given
        val fake = fakeNotificationService {
            sendNotification { _, _ -> /* no-op */ }
        }

        // When - call final method (not overridable)
        // This will print to console, but we're just verifying it doesn't throw
        fake.logNotification("user-123", "Test message")

        // Then - no exception thrown, method executes original logic
        // (In real tests, you might capture stdout to verify the log)
    }
}
