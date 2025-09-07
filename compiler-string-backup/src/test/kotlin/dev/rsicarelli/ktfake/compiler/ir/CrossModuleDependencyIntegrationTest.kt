// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.BeforeTest

/**
 * Integration tests for cross-module dependency system.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover end-to-end integration of dependency injection, metadata exchange,
 * and configuration access across module boundaries.
 *
 * Integration Requirements:
 * - Dependencies should be injected automatically
 * - Configuration access should work across modules
 * - Build order should be resolved correctly
 * - No duplicate fake generation
 */
class CrossModuleDependencyIntegrationTest {

    private lateinit var dependencyGenerator: CrossModuleDependencyGenerator
    private lateinit var metadataManager: DependencyMetadataManager
    private lateinit var integrator: CrossModuleDependencyIntegrator

    @BeforeTest
    fun setUp() {
        dependencyGenerator = CrossModuleDependencyGenerator()
        metadataManager = DependencyMetadataManager()
        integrator = CrossModuleDependencyIntegrator(dependencyGenerator, metadataManager)
    }

    @Test
    fun `GIVEN cross-module scenario WHEN generating complete system THEN should integrate all components`() {
        // Given: Multi-module system with dependencies
        val moduleStructure = mapOf(
            "order-module" to mapOf(
                "OrderService" to listOf("UserService", "AnalyticsService")
            ),
            "user-module" to mapOf(
                "UserService" to listOf("DatabaseService")
            ),
            "analytics-module" to mapOf(
                "AnalyticsService" to emptyList<String>()
            ),
            "database-module" to mapOf(
                "DatabaseService" to emptyList<String>()
            )
        )

        // When: Generating complete integrated system
        val integratedSystem = integrator.generateCompleteSystem(moduleStructure)

        // Then: Should integrate all components correctly
        assertTrue(integratedSystem.contains("OrderService"), "Should include OrderService")
        assertTrue(integratedSystem.contains("fakeUserService()"), "Should inject UserService dependency")
        assertTrue(integratedSystem.contains("fakeAnalyticsService()"), "Should inject AnalyticsService dependency")
        assertTrue(integratedSystem.contains("getUserService()"), "Should provide dependency access")
    }

    @Test
    fun `GIVEN dependency configuration WHEN accessing across modules THEN should provide chainable configuration`() {
        // Given: Cross-module dependency configuration
        val dependencies = listOf("UserService", "AnalyticsService")

        // When: Generating chainable configuration system
        val configSystem = integrator.generateChainableConfiguration(
            serviceName = "OrderService",
            dependencies = dependencies
        )

        // Then: Should provide fluent configuration API
        assertTrue(configSystem.contains("fun userService(configure: FakeUserServiceConfig.() -> Unit)"), "Should provide user service config")
        assertTrue(configSystem.contains("fun analyticsService(configure: FakeAnalyticsServiceConfig.() -> Unit)"), "Should provide analytics service config")
        assertTrue(configSystem.contains("getUserService().apply"), "Should apply configuration to dependencies")
    }

    @Test
    fun `GIVEN build order constraints WHEN compiling modules THEN should respect dependency order`() {
        // Given: Modules with complex dependency hierarchy
        val dependencyHierarchy = mapOf(
            "api-module" to emptyList<String>(),
            "database-module" to listOf("api-module"),
            "user-module" to listOf("database-module"),
            "order-module" to listOf("user-module", "analytics-module"),
            "analytics-module" to listOf("api-module")
        )

        // When: Determining build order
        val buildOrder = integrator.determineBuildOrder(dependencyHierarchy)

        // Then: Should respect all dependency constraints
        val apiIndex = buildOrder.indexOf("api-module")
        val databaseIndex = buildOrder.indexOf("database-module")
        val userIndex = buildOrder.indexOf("user-module")
        val analyticsIndex = buildOrder.indexOf("analytics-module")
        val orderIndex = buildOrder.indexOf("order-module")

        assertTrue(apiIndex < databaseIndex, "API should be built before database")
        assertTrue(apiIndex < analyticsIndex, "API should be built before analytics")
        assertTrue(databaseIndex < userIndex, "Database should be built before user")
        assertTrue(userIndex < orderIndex, "User should be built before order")
        assertTrue(analyticsIndex < orderIndex, "Analytics should be built before order")
    }

    @Test
    fun `GIVEN incremental compilation WHEN dependencies change THEN should rebuild affected modules only`() {
        // Given: Original system state
        val originalModules = mapOf(
            "order-module" to listOf("UserService"),
            "user-module" to emptyList<String>()
        )

        val updatedModules = mapOf(
            "order-module" to listOf("UserService", "AnalyticsService"), // Added dependency
            "user-module" to emptyList<String>(),
            "analytics-module" to emptyList<String>() // New module
        )

        // When: Detecting incremental changes
        val incrementalUpdate = integrator.calculateIncrementalUpdate(originalModules, updatedModules)

        // Then: Should identify only affected modules
        assertTrue(incrementalUpdate.affectedModules.contains("order-module"), "Should rebuild order module")
        assertTrue(incrementalUpdate.newModules.contains("analytics-module"), "Should build new analytics module")
        assertFalse(incrementalUpdate.affectedModules.contains("user-module"), "Should not rebuild unaffected user module")
    }

    @Test
    fun `GIVEN circular dependencies WHEN validating system THEN should detect and prevent cycles`() {
        // Given: System with circular dependency
        val circularSystem = mapOf(
            "service-a" to listOf("service-b"),
            "service-b" to listOf("service-c"),
            "service-c" to listOf("service-a")
        )

        // When: Validating system for circular dependencies
        val validation = integrator.validateSystemDependencies(circularSystem)

        // Then: Should detect and report circular dependencies
        assertTrue(validation.hasCircularDependencies, "Should detect circular dependency")
        assertTrue(validation.circularCycles.isNotEmpty(), "Should identify circular cycles")
        assertTrue(validation.isValid == false, "System should be invalid due to cycles")
    }

    @Test
    fun `GIVEN missing dependencies WHEN building system THEN should provide helpful error messages`() {
        // Given: System with missing dependencies
        val systemWithMissing = mapOf(
            "order-service" to listOf("user-service", "missing-service"),
            "user-service" to emptyList<String>()
        )

        // When: Validating system dependencies
        val validation = integrator.validateSystemDependencies(systemWithMissing)

        // Then: Should identify missing dependencies with helpful messages
        assertFalse(validation.isValid, "System should be invalid")
        assertTrue(validation.errors.any { it.contains("missing-service") }, "Should report missing service")
        assertTrue(validation.errors.any { it.contains("order-service") }, "Should identify which service has missing dependency")
    }

    @Test
    fun `GIVEN large dependency graph WHEN optimizing generation THEN should minimize duplicate work`() {
        // Given: Large system with many dependencies
        val largeSystem = (1..20).associate { i ->
            "module-$i" to if (i > 1) listOf("Service${i-1}") else emptyList()
        }

        // When: Generating optimized system
        val optimization = integrator.generateOptimizedSystem(largeSystem)

        // Then: Should optimize for minimal duplicate work
        assertTrue(optimization.generatedServices.size == 20, "Should generate all services")
        assertTrue(optimization.optimizationApplied, "Should apply optimizations")
        assertTrue(optimization.duplicateGenerationsAvoided > 0, "Should avoid duplicate generation")
    }

    @Test
    fun `GIVEN runtime configuration WHEN accessing dependency configurations THEN should provide type-safe access`() {
        // Given: Service with injected dependencies
        val serviceName = "OrderService"
        val dependencies = listOf("UserService", "AnalyticsService")

        // When: Generating runtime configuration access
        val runtimeConfig = integrator.generateRuntimeConfigurationAccess(serviceName, dependencies)

        // Then: Should provide type-safe dependency configuration
        assertTrue(runtimeConfig.contains("fun configureUserService"), "Should provide user service configuration")
        assertTrue(runtimeConfig.contains("fun configureAnalyticsService"), "Should provide analytics service configuration")
        assertTrue(runtimeConfig.contains("FakeUserServiceImpl"), "Should use concrete implementation types")
        assertTrue(runtimeConfig.contains("apply { configure() }"), "Should apply configuration safely")
    }

    @Test
    fun `GIVEN multi-platform target WHEN generating cross-module code THEN should be platform agnostic`() {
        // Given: Cross-platform module system
        val multiPlatformModules = mapOf(
            "common-service" to emptyList<String>(),
            "platform-service" to listOf("CommonService")
        )

        // When: Generating platform-agnostic code
        val platformCode = integrator.generateMultiPlatformCompatibleCode(multiPlatformModules)

        // Then: Should generate platform-agnostic dependency injection
        assertFalse(platformCode.contains("java."), "Should not contain Java-specific imports")
        assertFalse(platformCode.contains("System."), "Should not contain system-specific calls")
        assertTrue(platformCode.contains("expect"), "Should use expect/actual pattern if needed")
        assertTrue(platformCode.contains("internal"), "Should use appropriate visibility modifiers")
    }

    @Test
    fun `GIVEN performance requirements WHEN generating large systems THEN should optimize for compilation speed`() {
        // Given: Large system requiring performance optimization
        val largeSystem = (1..50).associate { i ->
            "service-$i" to (1..3).map { j -> "Dependency${i}_$j" }
        }

        // When: Generating with performance optimizations
        val optimizedGeneration = integrator.generateWithPerformanceOptimizations(largeSystem)

        // Then: Should apply performance optimizations
        assertTrue(optimizedGeneration.usesParallelGeneration, "Should use parallel generation")
        assertTrue(optimizedGeneration.cacheEnabled, "Should enable caching")
        assertTrue(optimizedGeneration.incrementalSupported, "Should support incremental updates")
        assertTrue(optimizedGeneration.generationTimeMs < 5000, "Should complete within reasonable time")
    }
}
