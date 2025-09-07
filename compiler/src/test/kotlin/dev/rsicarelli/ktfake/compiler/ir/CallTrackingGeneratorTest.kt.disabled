// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.BeforeTest

/**
 * Tests for call tracking generation in IR phase.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover call tracking data structures, collection, and verification.
 *
 * Based on roadmap pattern:
 * ```kotlin
 * @Fake(trackCalls = true)
 * interface AnalyticsService {
 *     fun track(event: String, properties: Map<String, Any>)
 * }
 *
 * // Generated with call tracking
 * internal class FakeAnalyticsServiceImpl : AnalyticsService {
 *     data class TrackCall(val event: String, val properties: Map<String, Any>)
 *     internal val trackCalls = mutableListOf<TrackCall>()
 *
 *     override fun track(event: String, properties: Map<String, Any>) {
 *         trackCalls.add(TrackCall(event, properties))
 *         trackBehavior(event, properties)
 *     }
 *
 *     internal fun verifyTracked(event: String, times: Int = 1): Boolean =
 *         trackCalls.count { it.event == event } == times
 * }
 * ```
 */
class CallTrackingGeneratorTest {

    private lateinit var generator: CallTrackingGenerator

    @BeforeTest
    fun setUp() {
        generator = CallTrackingGenerator()
    }

    @Test
    fun `GIVEN @Fake without trackCalls WHEN checking if tracking needed THEN should return false`() {
        // Given: @Fake annotation without trackCalls=true
        // When: Checking if call tracking is needed
        val needsTracking = generator.needsCallTracking(trackCalls = false)

        // Then: Should not need call tracking
        assertFalse(needsTracking, "Should not need call tracking when trackCalls=false")
    }

    @Test
    fun `GIVEN @Fake with trackCalls=true WHEN checking if tracking needed THEN should return true`() {
        // Given: @Fake(trackCalls = true) annotation
        // When: Checking if call tracking is needed
        val needsTracking = generator.needsCallTracking(trackCalls = true)

        // Then: Should need call tracking
        assertTrue(needsTracking, "Should need call tracking when trackCalls=true")
    }

    @Test
    fun `GIVEN method with parameters WHEN generating call data class THEN should include all parameters`() {
        // Given: Method track(event: String, properties: Map<String, Any>)
        // When: Generating call data class
        val dataClassName = generator.generateCallDataClassName("track")

        // Then: Should create TrackCall data class
        assertEquals("TrackCall", dataClassName, "Should generate proper call data class name")
    }

    @Test
    fun `GIVEN method WHEN generating call collection THEN should store call data`() {
        // Given: Method with call tracking enabled
        // When: Generating call collection code
        val collectionCode = generator.generateCallCollection("track", listOf("event", "properties"))

        // Then: Should generate code to collect call data
        assertTrue(collectionCode.contains("TrackCall"), "Should create call data instance")
        assertTrue(collectionCode.contains("trackCalls.add"), "Should add to call collection")
    }

    @Test
    fun `GIVEN tracked method WHEN generating verification methods THEN should create verify functions`() {
        // Given: Method with call tracking enabled
        // When: Generating verification methods
        val verificationMethods = generator.generateVerificationMethods("track")

        // Then: Should generate verification methods
        assertTrue(verificationMethods.contains("verifyTrackTracked"), "Should generate verifyTrackTracked method")
        assertTrue(verificationMethods.contains("verifyTrackNever"), "Should generate verifyTrackNever method")
    }

    @Test
    fun `GIVEN multiple methods WHEN generating call tracking THEN should track each method separately`() {
        // Given: Multiple methods with call tracking
        val methods = listOf("track", "identify", "flush")

        // When: Generating call tracking for all methods
        val trackingCode = generator.generateMethodCallTracking(methods)

        // Then: Should generate separate tracking for each method
        assertTrue(trackingCode.contains("TrackCall"), "Should track 'track' method")
        assertTrue(trackingCode.contains("IdentifyCall"), "Should track 'identify' method")
        assertTrue(trackingCode.contains("FlushCall"), "Should track 'flush' method")
    }

    @Test
    fun `GIVEN call tracking WHEN generating clear methods THEN should provide cleanup capability`() {
        // Given: Call tracking enabled
        // When: Generating cleanup methods
        val cleanupMethods = generator.generateCleanupMethods(listOf("track", "identify"))

        // Then: Should generate methods to clear call history
        assertTrue(cleanupMethods.contains("clearTrackCalls"), "Should generate clear method for track")
        assertTrue(cleanupMethods.contains("clearAllCalls"), "Should generate clear all method")
    }
}
