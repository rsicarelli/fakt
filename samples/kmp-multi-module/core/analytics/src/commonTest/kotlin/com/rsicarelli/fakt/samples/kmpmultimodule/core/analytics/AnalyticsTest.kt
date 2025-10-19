// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for Analytics fake generation and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsTest {

    @Test
    fun `GIVEN Analytics fake WHEN configuring track THEN should capture event and properties`() {
        // Given
        var capturedEvent = ""
        var capturedProperties: Map<String, Any> = emptyMap()

        val analytics =
            fakeAnalytics {
                track { eventName, properties ->
                    capturedEvent = eventName
                    capturedProperties = properties
                }
            }

        // When
        analytics.track("button_clicked", mapOf("screen" to "login", "buttonId" to "submit"))

        // Then
        assertEquals("button_clicked", capturedEvent)
        assertEquals("login", capturedProperties["screen"])
        assertEquals("submit", capturedProperties["buttonId"])
    }

    @Test
    fun `GIVEN Analytics fake WHEN configuring identify THEN should capture userId and properties`() {
        // Given
        var capturedUserId = ""
        var capturedProperties: Map<String, Any> = emptyMap()

        val analytics =
            fakeAnalytics {
                identify { userId, properties ->
                    capturedUserId = userId
                    capturedProperties = properties
                }
            }

        // When
        analytics.identify("user-123", mapOf("email" to "test@example.com", "plan" to "premium"))

        // Then
        assertEquals("user-123", capturedUserId)
        assertEquals("test@example.com", capturedProperties["email"])
        assertEquals("premium", capturedProperties["plan"])
    }

    @Test
    fun `GIVEN Analytics fake WHEN configuring setUserProperties THEN should capture properties`() {
        // Given
        var capturedProperties: Map<String, Any> = emptyMap()

        val analytics =
            fakeAnalytics {
                setUserProperties { properties ->
                    capturedProperties = properties
                }
            }

        // When
        analytics.setUserProperties(mapOf("theme" to "dark", "language" to "pt-BR"))

        // Then
        assertEquals("dark", capturedProperties["theme"])
        assertEquals("pt-BR", capturedProperties["language"])
    }

    @Test
    fun `GIVEN Analytics fake WHEN configuring isEnabled THEN should return configured value`() {
        // Given
        val analytics =
            fakeAnalytics {
                isEnabled { true }
            }

        // When
        val enabled = analytics.isEnabled

        // Then
        assertTrue(enabled)
    }
}

/**
 * Tests for PerformanceMonitor fake generation and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceMonitorTest {

    @Test
    fun `GIVEN PerformanceMonitor fake WHEN starting trace THEN should return trace ID`() {
        // Given
        val monitor =
            fakePerformanceMonitor {
                startTrace { traceName ->
                    "trace-$traceName-123"
                }
            }

        // When
        val traceId = monitor.startTrace("screen_load")

        // Then
        assertEquals("trace-screen_load-123", traceId)
    }

    @Test
    fun `GIVEN PerformanceMonitor fake WHEN stopping trace THEN should capture trace ID`() {
        // Given
        var stoppedTraceId = ""
        val monitor =
            fakePerformanceMonitor {
                stopTrace { traceId ->
                    stoppedTraceId = traceId
                }
            }

        // When
        monitor.stopTrace("trace-123")

        // Then
        assertEquals("trace-123", stoppedTraceId)
    }

    @Test
    fun `GIVEN PerformanceMonitor fake WHEN recording metric THEN should capture metric details`() {
        // Given
        var capturedMetricName = ""
        var capturedValue = 0.0
        var capturedAttributes: Map<String, String> = emptyMap()

        val monitor =
            fakePerformanceMonitor {
                recordMetric { metricName, value, attributes ->
                    capturedMetricName = metricName
                    capturedValue = value
                    capturedAttributes = attributes
                }
            }

        // When
        monitor.recordMetric("api_response_time", 250.5, mapOf("endpoint" to "/users"))

        // Then
        assertEquals("api_response_time", capturedMetricName)
        assertEquals(250.5, capturedValue)
        assertEquals("/users", capturedAttributes["endpoint"])
    }

    @Test
    fun `GIVEN PerformanceMonitor fake WHEN reporting error THEN should capture error details`() {
        // Given
        var capturedMessage = ""
        var capturedThrowable: Throwable? = null
        var capturedAttributes: Map<String, String> = emptyMap()

        val monitor =
            fakePerformanceMonitor {
                reportError { message, throwable, attributes ->
                    capturedMessage = message
                    capturedThrowable = throwable
                    capturedAttributes = attributes
                }
            }

        val testException = RuntimeException("Network error")

        // When
        monitor.reportError("Failed to fetch data", testException, mapOf("endpoint" to "/api/orders"))

        // Then
        assertEquals("Failed to fetch data", capturedMessage)
        assertNotNull(capturedThrowable)
        assertEquals("Network error", capturedThrowable?.message)
        assertEquals("/api/orders", capturedAttributes["endpoint"])
    }

    @Test
    fun `GIVEN PerformanceMonitor fake WHEN configuring isEnabled THEN should return configured value`() {
        // Given
        val monitor =
            fakePerformanceMonitor {
                isEnabled { true }
            }

        // When
        val enabled = monitor.isEnabled

        // Then
        assertTrue(enabled)
    }
}
