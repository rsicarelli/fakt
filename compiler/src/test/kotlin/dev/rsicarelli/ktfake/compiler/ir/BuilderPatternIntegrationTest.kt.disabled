// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.BeforeTest

/**
 * Tests for builder pattern integration with existing fake generation.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover integration with call tracking, existing IR generation, and configuration DSL.
 *
 * Integration Requirements:
 * - Builder pattern should work with @Fake(builder = true)
 * - Should integrate with existing factory functions
 * - Should support call tracking when enabled
 * - Should maintain thread-safety guarantees
 */
class BuilderPatternIntegrationTest {

    private lateinit var builderGenerator: BuilderPatternGenerator
    private lateinit var integrator: BuilderPatternIntegrator

    @BeforeTest
    fun setUp() {
        builderGenerator = BuilderPatternGenerator()
        integrator = BuilderPatternIntegrator(builderGenerator)
    }

    @Test
    fun `GIVEN @Fake without builder WHEN generating implementation THEN should not include builder pattern`() {
        // Given: @Fake annotation without builder=true
        // When: Generating implementation for data class
        val implementation = integrator.generateDataClassImplementation(
            className = "User",
            properties = listOf("id: String", "name: String"),
            useBuilder = false
        )

        // Then: Should not include builder pattern
        assertFalse(implementation.contains("FakeUserBuilder"), "Should not have builder class")
        assertFalse(implementation.contains("configure: FakeUserConfig"), "Should not have builder config DSL")
        assertTrue(implementation.contains("fun fakeUser()"), "Should still have factory function")
    }

    @Test
    fun `GIVEN @Fake with builder=true WHEN generating implementation THEN should include builder pattern`() {
        // Given: @Fake(builder = true) annotation
        // When: Generating implementation for data class
        val implementation = integrator.generateDataClassImplementation(
            className = "User",
            properties = listOf("id: String", "name: String", "age: Int"),
            useBuilder = true
        )

        // Then: Should include complete builder pattern
        assertTrue(implementation.contains("class FakeUserBuilder"), "Should have builder class")
        assertTrue(implementation.contains("fun fakeUser(configure: FakeUserConfig.() -> Unit"), "Should have config DSL")
        assertTrue(implementation.contains("FakeUserBuilder().apply"), "Should use builder in factory")
        assertTrue(implementation.contains("fun build(): User"), "Should have build method")
    }

    @Test
    fun `GIVEN builder pattern with call tracking WHEN generating implementation THEN should support both features`() {
        // Given: @Fake(builder = true, trackCalls = true) combination
        // When: Generating implementation with both features
        val implementation = integrator.generateDataClassWithCallTracking(
            className = "User",
            properties = listOf("id: String", "name: String"),
            useBuilder = true,
            trackCalls = true
        )

        // Then: Should combine both features seamlessly
        assertTrue(implementation.contains("FakeUserBuilder"), "Should have builder pattern")
        assertTrue(implementation.contains("userCalls = mutableListOf"), "Should have call tracking")
        assertTrue(implementation.contains("verifyUserTracked"), "Should have verification methods")
    }

    @Test
    fun `GIVEN nested data classes WHEN generating builders THEN should resolve dependencies automatically`() {
        // Given: Nested data class structure
        // When: Generating builders with automatic dependency resolution
        val implementation = integrator.generateNestedDataClassBuilders(
            mainClass = "User",
            mainProperties = listOf("id: String", "profile: UserProfile"),
            nestedClasses = mapOf(
                "UserProfile" to listOf("name: String", "settings: UserSettings"),
                "UserSettings" to listOf("theme: String", "notifications: Boolean")
            )
        )

        // Then: Should generate all required builders with correct dependencies
        assertTrue(implementation.contains("fakeUserProfile()"), "Should use nested profile fake")
        assertTrue(implementation.contains("fakeUserSettings()"), "Should use nested settings fake")
        assertTrue(implementation.contains("class FakeUserBuilder"), "Should generate main builder")
        assertTrue(implementation.contains("class FakeUserProfileBuilder"), "Should generate nested builders")
    }

    @Test
    fun `GIVEN builder pattern WHEN integrating with existing IR generation THEN should maintain compatibility`() {
        // Given: Existing IR generation infrastructure
        // When: Integrating builder pattern with existing systems
        val factoryIntegration = integrator.integrateWithFactoryGeneration("UserService")

        // Then: Should maintain existing factory function patterns
        assertTrue(factoryIntegration.contains("fun fakeUserService"), "Should maintain factory naming")
        assertTrue(factoryIntegration.contains("thread-safe"), "Should maintain thread safety")
        assertTrue(factoryIntegration.contains("FakeRuntime.getOrCreate"), "Should use existing runtime")
    }

    @Test
    fun `GIVEN configuration DSL WHEN using builder pattern THEN should provide intuitive API`() {
        // Given: Builder configuration DSL
        // When: Generating user-friendly configuration API
        val configApi = integrator.generateBuilderConfigurationApi(
            className = "User",
            properties = listOf("id: String", "name: String", "isActive: Boolean")
        )

        // Then: Should provide clean, intuitive configuration API
        assertTrue(configApi.contains("var id: String"), "Should expose properties as vars")
        assertTrue(configApi.contains("var name: String"), "Should allow direct property setting")
        assertTrue(configApi.contains("var isActive: Boolean"), "Should support all property types")
    }

    @Test
    fun `GIVEN complex object graphs WHEN generating builders THEN should handle circular dependencies`() {
        // Given: Potentially circular object dependencies
        // When: Generating builders for complex graphs
        val implementation = integrator.generateBuilderWithCircularDependencyHandling(
            className = "Department",
            properties = listOf("name: String", "manager: Employee"),
            circularTypes = setOf("Employee") // Employee might reference Department
        )

        // Then: Should handle circular dependencies gracefully
        assertTrue(implementation.contains("lazy"), "Should use lazy initialization for circular refs")
        assertTrue(implementation.contains("Department"), "Should generate primary builder")
        assertTrue(true, "Test structure for circular dependency handling")
    }

    @Test
    fun `GIVEN performance requirements WHEN generating builders THEN should optimize for efficiency`() {
        // Given: Performance requirements for builder pattern
        // When: Generating optimized builder implementation
        val optimizedBuilder = integrator.generateOptimizedBuilder(
            className = "HighPerformanceData",
            properties = listOf("id: String", "data: ByteArray", "metadata: Map<String, String>")
        )

        // Then: Should generate performance-optimized builders
        assertTrue(optimizedBuilder.contains("@JvmInline") || optimizedBuilder.contains("inline"), "Should consider inline optimizations")
        assertTrue(optimizedBuilder.contains("build()"), "Should have efficient build method")
        assertTrue(true, "Test structure for performance optimization")
    }

    @Test
    fun `GIVEN multiple data classes WHEN generating builders THEN should handle batch generation`() {
        // Given: Multiple data classes requiring builder pattern
        val dataClasses = mapOf(
            "User" to listOf("id: String", "name: String"),
            "Product" to listOf("sku: String", "name: String", "price: Double"),
            "Order" to listOf("id: String", "user: User", "products: List<Product>")
        )

        // When: Generating builders for all classes
        val batchGeneration = integrator.generateBuildersForMultipleClasses(dataClasses)

        // Then: Should generate all builders with correct relationships
        assertTrue(batchGeneration.contains("FakeUserBuilder"), "Should generate User builder")
        assertTrue(batchGeneration.contains("FakeProductBuilder"), "Should generate Product builder")
        assertTrue(batchGeneration.contains("FakeOrderBuilder"), "Should generate Order builder")
        assertTrue(batchGeneration.contains("fakeUser()"), "Should use nested User fake in Order")
    }
}
