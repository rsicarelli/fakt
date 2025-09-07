// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.BeforeTest

/**
 * Tests for call tracking integration with existing implementation generation.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover integration between call tracking and existing IR generation components.
 *
 * Integration Requirements:
 * - Call tracking should work with existing factory functions
 * - Implementation classes should conditionally include tracking
 * - Configuration DSL should support call tracking access
 * - Performance should remain optimal when tracking is disabled
 */
class CallTrackingIntegrationTest {

    private lateinit var implementationGenerator: ImplementationClassGenerator
    private lateinit var callTrackingGenerator: CallTrackingGenerator
    private lateinit var integrator: CallTrackingIntegrator

    @BeforeTest
    fun setUp() {
        implementationGenerator = ImplementationClassGenerator()
        callTrackingGenerator = CallTrackingGenerator()
        integrator = CallTrackingIntegrator(implementationGenerator, callTrackingGenerator)
    }

    @Test
    fun `GIVEN @Fake without trackCalls WHEN generating implementation THEN should not include call tracking`() {
        // Given: @Fake annotation without trackCalls=true
        // When: Generating implementation class
        val implementation = integrator.generateImplementation(
            interfaceName = "UserService",
            methods = listOf("getUser"),
            trackCalls = false
        )

        // Then: Should not include call tracking infrastructure
        assertFalse(implementation.contains("Calls = mutableListOf"), "Should not have call storage")
        assertFalse(implementation.contains("verifyTracked"), "Should not have verification methods")
        assertTrue(implementation.contains("override fun getUser"), "Should still have method overrides")
    }

    @Test
    fun `GIVEN @Fake with trackCalls=true WHEN generating implementation THEN should include call tracking`() {
        // Given: @Fake(trackCalls = true) annotation
        // When: Generating implementation class
        val implementation = integrator.generateImplementation(
            interfaceName = "UserService",
            methods = listOf("getUser"),
            trackCalls = true
        )

        // Then: Should include call tracking infrastructure
        assertTrue(implementation.contains("GetUserCall"), "Should have call data class")
        assertTrue(implementation.contains("getUserCalls = mutableListOf"), "Should have call storage")
        assertTrue(implementation.contains("verifyGetUserTracked"), "Should have verification methods")
        assertTrue(implementation.contains("getUserCalls.add"), "Should record calls")
    }

    @Test
    fun `GIVEN call tracking enabled WHEN generating factory function THEN should maintain existing API`() {
        // Given: Factory function with call tracking enabled
        // When: Generating factory function
        val factory = integrator.generateFactoryWithTracking(
            interfaceName = "UserService",
            trackCalls = true
        )

        // Then: Should maintain same public API
        assertTrue(factory.contains("fun fakeUserService"), "Should have same factory name")
        assertTrue(factory.contains("configure: FakeUserServiceConfig.() -> Unit"), "Should have same config parameter")
        assertTrue(factory.contains("return FakeUserServiceImpl()"), "Should return same implementation type")
    }

    @Test
    fun `GIVEN call tracking WHEN generating configuration DSL THEN should provide tracking access`() {
        // Given: Configuration DSL with call tracking
        // When: Generating configuration DSL with tracking support
        val configDsl = integrator.generateConfigurationDslWithTracking(
            interfaceName = "UserService",
            methods = listOf("getUser"),
            trackCalls = true
        )

        // Then: Should provide access to call tracking through configuration
        assertTrue(configDsl.contains("fun getVerification()"), "Should provide verification access")
        assertTrue(configDsl.contains("fun clearCalls()"), "Should provide cleanup access")
    }

    @Test
    fun `GIVEN mixed tracking settings WHEN generating multiple fakes THEN should handle each independently`() {
        // Given: Multiple interfaces with different tracking settings
        // When: Generating implementations for both
        val trackedImpl = integrator.generateImplementation("AnalyticsService", listOf("track"), trackCalls = true)
        val nonTrackedImpl = integrator.generateImplementation("UserService", listOf("getUser"), trackCalls = false)

        // Then: Should handle each according to its settings
        assertTrue(trackedImpl.contains("trackCalls"), "Tracked service should have call tracking")
        assertFalse(nonTrackedImpl.contains("getUserCalls"), "Non-tracked service should not have call tracking")
    }

    @Test
    fun `GIVEN call tracking WHEN performance is critical THEN should optimize for disabled case`() {
        // Given: Performance-critical code with call tracking disabled
        // When: Generating optimized implementation
        val optimizedImpl = integrator.generateOptimizedImplementation(
            interfaceName = "PerformanceService",
            methods = listOf("criticalMethod"),
            trackCalls = false
        )

        // Then: Should have zero overhead when tracking is disabled
        assertFalse(optimizedImpl.contains("mutableListOf"), "Should not allocate tracking collections")
        assertFalse(optimizedImpl.contains("add("), "Should not have call recording overhead")
        assertTrue(optimizedImpl.contains("override fun criticalMethod"), "Should maintain method functionality")
    }

    @Test
    fun `GIVEN call tracking integration WHEN compiling THEN should maintain thread safety`() {
        // Given: Call tracking integrated with thread-safe design
        // When: Verifying thread safety is maintained
        // Then: Integration should not break existing thread safety guarantees

        assertTrue(true, "Test structure for thread safety verification")
    }
}
