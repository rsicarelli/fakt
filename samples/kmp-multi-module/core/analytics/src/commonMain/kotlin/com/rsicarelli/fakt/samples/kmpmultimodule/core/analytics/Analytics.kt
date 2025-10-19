// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics

import com.rsicarelli.fakt.Fake

/**
 * Analytics interface for tracking user events and metrics.
 *
 * Separate from logging as analytics data typically goes to different systems
 * (e.g., Mixpanel, Amplitude, Firebase Analytics).
 *
 * Example usage:
 * ```kotlin
 * analytics.track("button_clicked", mapOf("screen" to "login", "buttonId" to "submit"))
 * analytics.identify("user-123", mapOf("email" to "user@example.com"))
 * ```
 */
@Fake
interface Analytics {
    /**
     * Track a user event with properties.
     */
    fun track(eventName: String, properties: Map<String, Any> = emptyMap())

    /**
     * Identify a user with their unique ID and properties.
     */
    fun identify(userId: String, properties: Map<String, Any> = emptyMap())

    /**
     * Set user properties that persist across sessions.
     */
    fun setUserProperties(properties: Map<String, Any>)

    /**
     * Whether analytics tracking is enabled.
     */
    val isEnabled: Boolean
}

/**
 * Performance monitoring interface for tracking app performance metrics.
 *
 * Used to track things like screen load times, API response times, crash reports.
 */
@Fake
interface PerformanceMonitor {
    /**
     * Start tracking a performance metric.
     * Returns a trace ID that can be used to stop the tracking.
     */
    fun startTrace(traceName: String): String

    /**
     * Stop tracking a performance metric.
     */
    fun stopTrace(traceId: String)

    /**
     * Record a custom metric value.
     */
    fun recordMetric(metricName: String, value: Double, attributes: Map<String, String> = emptyMap())

    /**
     * Report an error or exception.
     */
    fun reportError(message: String, throwable: Throwable? = null, attributes: Map<String, String> = emptyMap())

    /**
     * Whether performance monitoring is enabled.
     */
    val isEnabled: Boolean
}
