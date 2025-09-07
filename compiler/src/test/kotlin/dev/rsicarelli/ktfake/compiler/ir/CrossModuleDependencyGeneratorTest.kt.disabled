// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.BeforeTest

/**
 * Tests for cross-module dependency generation.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover dependency injection, metadata exchange, and configuration access.
 *
 * Based on usage patterns:
 * ```kotlin
 * @Fake(dependencies = [UserService::class, AnalyticsService::class])
 * interface OrderService {
 *     suspend fun createOrder(userId: String): Order
 * }
 *
 * // Generated usage:
 * val orderService = fakeOrderService()
 * orderService.getUserService().configureGetUser { User.premium() }
 * ```
 */
class CrossModuleDependencyGeneratorTest {

    private lateinit var generator: CrossModuleDependencyGenerator

    @BeforeTest
    fun setUp() {
        generator = CrossModuleDependencyGenerator()
    }

    @Test
    fun `GIVEN interface with dependencies WHEN generating implementation THEN should inject dependencies`() {
        // Given: Interface with dependencies annotation
        val dependencies = listOf("UserService", "AnalyticsService")

        // When: Generating implementation with dependencies
        val implementation = generator.generateImplementationWithDependencies(
            interfaceName = "OrderService",
            dependencies = dependencies
        )

        // Then: Should inject dependencies as private fields
        assertTrue(implementation.contains("private val userService: UserService"), "Should inject UserService dependency")
        assertTrue(implementation.contains("private val analyticsService: AnalyticsService"), "Should inject AnalyticsService dependency")
        assertTrue(implementation.contains("fakeUserService()"), "Should use fake factory for UserService")
        assertTrue(implementation.contains("fakeAnalyticsService()"), "Should use fake factory for AnalyticsService")
    }

    @Test
    fun `GIVEN injected dependencies WHEN generating configuration access THEN should provide accessor methods`() {
        // Given: Implementation with injected dependencies
        val dependencies = listOf("UserService", "AnalyticsService")

        // When: Generating configuration access methods
        val configAccess = generator.generateDependencyConfigurationAccess(
            interfaceName = "OrderService",
            dependencies = dependencies
        )

        // Then: Should provide typed accessor methods
        assertTrue(configAccess.contains("fun getUserService(): FakeUserServiceImpl"), "Should provide UserService accessor")
        assertTrue(configAccess.contains("fun getAnalyticsService(): FakeAnalyticsServiceImpl"), "Should provide AnalyticsService accessor")
        assertTrue(configAccess.contains("userService as FakeUserServiceImpl"), "Should cast to implementation type")
    }

    @Test
    fun `GIVEN cross-module dependencies WHEN generating metadata THEN should create dependency manifest`() {
        // Given: Multiple interfaces with cross-module dependencies
        val serviceInfos = mapOf(
            "OrderService" to listOf("UserService", "AnalyticsService"),
            "UserService" to listOf("DatabaseService"),
            "AnalyticsService" to emptyList()
        )

        // When: Generating dependency metadata
        val metadata = generator.generateDependencyMetadata(serviceInfos)

        // Then: Should create complete dependency graph metadata
        assertTrue(metadata.contains("OrderService"), "Should include OrderService in metadata")
        assertTrue(metadata.contains("UserService"), "Should include UserService in metadata")
        assertTrue(metadata.contains("dependencies"), "Should include dependency information")
        assertTrue(metadata.contains("DatabaseService"), "Should include transitive dependencies")
    }

    @Test
    fun `GIVEN dependency resolution WHEN detecting circular dependencies THEN should handle gracefully`() {
        // Given: Circular dependency scenario
        val circularDeps = mapOf(
            "ServiceA" to listOf("ServiceB"),
            "ServiceB" to listOf("ServiceC"),
            "ServiceC" to listOf("ServiceA")
        )

        // When: Analyzing dependency graph for circular references
        val analysis = generator.analyzeDependencyGraph(circularDeps)

        // Then: Should detect and report circular dependencies
        assertTrue(analysis.hasCircularDependencies, "Should detect circular dependency")
        assertTrue(analysis.circularPaths.isNotEmpty(), "Should identify circular paths")
        assertEquals(listOf("ServiceA", "ServiceB", "ServiceC", "ServiceA"), analysis.circularPaths.first(), "Should identify complete cycle")
    }

    @Test
    fun `GIVEN build order requirements WHEN resolving dependencies THEN should determine correct build order`() {
        // Given: Complex dependency hierarchy
        val dependencies = mapOf(
            "OrderService" to listOf("UserService", "AnalyticsService"),
            "UserService" to listOf("DatabaseService", "CacheService"),
            "AnalyticsService" to listOf("CacheService"),
            "DatabaseService" to emptyList(),
            "CacheService" to emptyList()
        )

        // When: Resolving build order
        val buildOrder = generator.resolveBuildOrder(dependencies)

        // Then: Should determine correct topological order
        val databaseIndex = buildOrder.indexOf("DatabaseService")
        val cacheIndex = buildOrder.indexOf("CacheService")
        val userIndex = buildOrder.indexOf("UserService")
        val analyticsIndex = buildOrder.indexOf("AnalyticsService")
        val orderIndex = buildOrder.indexOf("OrderService")

        assertTrue(databaseIndex < userIndex, "DatabaseService should be built before UserService")
        assertTrue(cacheIndex < userIndex, "CacheService should be built before UserService")
        assertTrue(cacheIndex < analyticsIndex, "CacheService should be built before AnalyticsService")
        assertTrue(userIndex < orderIndex, "UserService should be built before OrderService")
        assertTrue(analyticsIndex < orderIndex, "AnalyticsService should be built before OrderService")
    }

    @Test
    fun `GIVEN no dependencies WHEN generating implementation THEN should not inject any dependencies`() {
        // Given: Interface without dependencies
        val dependencies = emptyList<String>()

        // When: Generating implementation
        val implementation = generator.generateImplementationWithDependencies(
            interfaceName = "SimpleService",
            dependencies = dependencies
        )

        // Then: Should not contain dependency injection
        assertFalse(implementation.contains("private val"), "Should not have injected dependencies")
        assertFalse(implementation.contains("fake"), "Should not have fake factory calls")
        assertTrue(implementation.contains("class FakeSimpleServiceImpl"), "Should still generate implementation")
    }

    @Test
    fun `GIVEN dependency injection WHEN generating factory function THEN should maintain same public API`() {
        // Given: Service with dependencies
        val dependencies = listOf("UserService")

        // When: Generating factory function
        val factory = generator.generateFactoryWithDependencyInjection(
            interfaceName = "OrderService",
            dependencies = dependencies
        )

        // Then: Should maintain same factory function signature
        assertTrue(factory.contains("fun fakeOrderService("), "Should have same factory name")
        assertTrue(factory.contains("configure: FakeOrderServiceConfig.() -> Unit = {}"), "Should have same config parameter")
        assertTrue(factory.contains("return FakeOrderServiceImpl()"), "Should return implementation")
    }

    @Test
    fun `GIVEN dependency configuration WHEN accessing nested dependencies THEN should provide chainable API`() {
        // Given: Nested dependency configuration
        val dependencies = listOf("UserService")

        // When: Generating chainable configuration API
        val configApi = generator.generateChainableConfigurationApi(
            interfaceName = "OrderService",
            dependencies = dependencies
        )

        // Then: Should provide chainable dependency access
        assertTrue(configApi.contains("fun userService(configure: FakeUserServiceConfig.() -> Unit)"), "Should provide dependency config")
        assertTrue(configApi.contains("getUserService().apply"), "Should apply configuration to dependency")
        assertTrue(configApi.contains("FakeUserServiceConfig"), "Should use dependency's config type")
    }

    @Test
    fun `GIVEN module boundaries WHEN generating cross-module access THEN should respect visibility`() {
        // Given: Cross-module dependency scenario
        val dependencies = listOf("ExternalService")

        // When: Generating cross-module implementation
        val implementation = generator.generateCrossModuleImplementation(
            interfaceName = "InternalService",
            dependencies = dependencies,
            isExternalDependency = true
        )

        // Then: Should respect module visibility rules
        assertTrue(implementation.contains("internal"), "Should use internal visibility for generated classes")
        assertTrue(implementation.contains("public fun fakeInternalService"), "Should expose public factory function")
        assertFalse(implementation.contains("private val externalService"), "Should not make external deps private")
    }

    @Test
    fun `GIVEN dependency versions WHEN resolving across modules THEN should handle version compatibility`() {
        // Given: Dependencies with version information
        val versionedDependencies = mapOf(
            "UserService" to "1.0.0",
            "AnalyticsService" to "2.1.0"
        )

        // When: Generating versioned dependency resolution
        val resolution = generator.generateVersionedDependencyResolution(
            interfaceName = "OrderService",
            dependencies = versionedDependencies
        )

        // Then: Should include version compatibility checks
        assertTrue(resolution.contains("// Compatible with UserService 1.0.0"), "Should document version compatibility")
        assertTrue(resolution.contains("// Compatible with AnalyticsService 2.1.0"), "Should document version compatibility")
        assertTrue(resolution.contains("checkCompatibility"), "Should include compatibility verification")
    }
}
