// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.BeforeTest

/**
 * Tests for dependency metadata management across modules.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover metadata serialization, cross-module exchange, and build integration.
 *
 * Metadata Requirements:
 * - Serialize/deserialize dependency information
 * - Exchange metadata between compilation units
 * - Support incremental compilation
 * - Handle missing dependencies gracefully
 */
class DependencyMetadataManagerTest {

    private lateinit var metadataManager: DependencyMetadataManager

    @BeforeTest
    fun setUp() {
        metadataManager = DependencyMetadataManager()
    }

    @Test
    fun `GIVEN dependency information WHEN serializing metadata THEN should create portable format`() {
        // Given: Dependency information for a service
        val serviceInfo = DependencyServiceInfo(
            serviceName = "OrderService",
            dependencies = listOf("UserService", "AnalyticsService"),
            moduleName = "order-module",
            packageName = "com.example.order"
        )

        // When: Serializing metadata
        val serialized = metadataManager.serializeDependencyInfo(serviceInfo)

        // Then: Should create portable metadata format
        assertTrue(serialized.contains("OrderService"), "Should include service name")
        assertTrue(serialized.contains("UserService"), "Should include dependencies")
        assertTrue(serialized.contains("order-module"), "Should include module information")
        assertTrue(serialized.isNotEmpty(), "Should generate non-empty metadata")
    }

    @Test
    fun `GIVEN serialized metadata WHEN deserializing THEN should recreate dependency information`() {
        // Given: Serialized metadata
        val originalInfo = DependencyServiceInfo(
            serviceName = "UserService",
            dependencies = listOf("DatabaseService"),
            moduleName = "user-module",
            packageName = "com.example.user"
        )
        val serialized = metadataManager.serializeDependencyInfo(originalInfo)

        // When: Deserializing metadata
        val deserialized = metadataManager.deserializeDependencyInfo(serialized)

        // Then: Should recreate original information
        assertEquals(originalInfo.serviceName, deserialized.serviceName, "Should preserve service name")
        assertEquals(originalInfo.dependencies, deserialized.dependencies, "Should preserve dependencies")
        assertEquals(originalInfo.moduleName, deserialized.moduleName, "Should preserve module name")
        assertEquals(originalInfo.packageName, deserialized.packageName, "Should preserve package name")
    }

    @Test
    fun `GIVEN multiple modules WHEN exchanging metadata THEN should aggregate dependency graph`() {
        // Given: Metadata from multiple modules
        val orderMetadata = createServiceMetadata("OrderService", listOf("UserService", "AnalyticsService"))
        val userMetadata = createServiceMetadata("UserService", listOf("DatabaseService"))
        val analyticsMetadata = createServiceMetadata("AnalyticsService", emptyList())

        // When: Aggregating metadata from multiple modules
        val aggregated = metadataManager.aggregateModuleMetadata(
            listOf(orderMetadata, userMetadata, analyticsMetadata)
        )

        // Then: Should create complete dependency graph
        assertEquals(3, aggregated.services.size, "Should include all services")
        assertTrue(aggregated.hasService("OrderService"), "Should include OrderService")
        assertTrue(aggregated.hasService("UserService"), "Should include UserService")
        assertTrue(aggregated.hasDependency("OrderService", "UserService"), "Should preserve dependencies")
    }

    @Test
    fun `GIVEN missing dependencies WHEN validating metadata THEN should identify missing services`() {
        // Given: Metadata with missing dependency
        val serviceInfo = createServiceMetadata("OrderService", listOf("UserService", "MissingService"))
        val availableServices = setOf("OrderService", "UserService", "AnalyticsService")

        // When: Validating dependencies
        val validation = metadataManager.validateDependencies(serviceInfo, availableServices)

        // Then: Should identify missing dependencies
        assertNotNull(validation.missingDependencies, "Should identify missing dependencies")
        assertTrue(validation.missingDependencies.contains("MissingService"), "Should identify MissingService as missing")
        assertEquals(1, validation.missingDependencies.size, "Should identify correct number of missing deps")
    }

    @Test
    fun `GIVEN incremental compilation WHEN metadata changes THEN should detect affected modules`() {
        // Given: Original and updated metadata
        val originalServices = listOf(
            createServiceMetadata("OrderService", listOf("UserService")),
            createServiceMetadata("UserService", emptyList())
        )
        val updatedServices = listOf(
            createServiceMetadata("OrderService", listOf("UserService", "AnalyticsService")),
            createServiceMetadata("UserService", emptyList()),
            createServiceMetadata("AnalyticsService", emptyList())
        )

        // When: Detecting changes for incremental compilation
        val changeDetection = metadataManager.detectMetadataChanges(originalServices, updatedServices)

        // Then: Should identify affected modules
        assertTrue(changeDetection.hasChanges, "Should detect changes")
        assertTrue(changeDetection.affectedServices.contains("OrderService"), "Should identify OrderService as affected")
        assertTrue(changeDetection.newServices.contains("AnalyticsService"), "Should identify new services")
    }

    @Test
    fun `GIVEN build order requirements WHEN analyzing metadata THEN should determine compilation order`() {
        // Given: Complex dependency metadata
        val services = listOf(
            createServiceMetadata("OrderService", listOf("UserService", "AnalyticsService")),
            createServiceMetadata("UserService", listOf("DatabaseService")),
            createServiceMetadata("AnalyticsService", listOf("CacheService")),
            createServiceMetadata("DatabaseService", emptyList()),
            createServiceMetadata("CacheService", emptyList())
        )

        // When: Determining build order
        val buildOrder = metadataManager.determineBuildOrder(services)

        // Then: Should provide correct topological order
        val databaseIndex = buildOrder.indexOf("DatabaseService")
        val cacheIndex = buildOrder.indexOf("CacheService")
        val userIndex = buildOrder.indexOf("UserService")
        val analyticsIndex = buildOrder.indexOf("AnalyticsService")
        val orderIndex = buildOrder.indexOf("OrderService")

        assertTrue(databaseIndex < userIndex, "DatabaseService should be built before UserService")
        assertTrue(cacheIndex < analyticsIndex, "CacheService should be built before AnalyticsService")
        assertTrue(userIndex < orderIndex, "UserService should be built before OrderService")
        assertTrue(analyticsIndex < orderIndex, "AnalyticsService should be built before OrderService")
    }

    @Test
    fun `GIVEN metadata persistence WHEN saving to build cache THEN should support incremental builds`() {
        // Given: Metadata to persist
        val services = listOf(
            createServiceMetadata("UserService", listOf("DatabaseService")),
            createServiceMetadata("DatabaseService", emptyList())
        )

        // When: Persisting metadata to build cache
        val persistedData = metadataManager.persistMetadataForIncremental(services)

        // Then: Should create cache-friendly format
        assertTrue(persistedData.contains("timestamp"), "Should include timestamp for cache validation")
        assertTrue(persistedData.contains("checksum"), "Should include checksum for change detection")
        assertTrue(persistedData.contains("dependencies"), "Should include dependency information")
        assertTrue(persistedData.length > 100, "Should contain substantial metadata")
    }

    @Test
    fun `GIVEN cross-module visibility WHEN generating metadata THEN should respect access modifiers`() {
        // Given: Services with different visibility levels
        val publicService = createServiceMetadata("PublicService", emptyList(), isPublic = true)
        val internalService = createServiceMetadata("InternalService", emptyList(), isPublic = false)

        // When: Generating cross-module metadata
        val crossModuleMetadata = metadataManager.generateCrossModuleMetadata(
            listOf(publicService, internalService),
            targetModule = "external-module"
        )

        // Then: Should only expose public services
        assertTrue(crossModuleMetadata.containsService("PublicService"), "Should expose public service")
        assertTrue(!crossModuleMetadata.containsService("InternalService"), "Should not expose internal service")
    }

    @Test
    fun `GIVEN metadata versioning WHEN handling compatibility THEN should manage version conflicts`() {
        // Given: Metadata with version information
        val v1Metadata = createVersionedServiceMetadata("UserService", "1.0.0", listOf("DatabaseService"))
        val v2Metadata = createVersionedServiceMetadata("UserService", "2.0.0", listOf("DatabaseService", "CacheService"))

        // When: Handling version compatibility
        val compatibility = metadataManager.checkVersionCompatibility(v1Metadata, v2Metadata)

        // Then: Should provide compatibility information
        assertTrue(compatibility.hasBreakingChanges, "Should detect breaking changes")
        assertEquals("2.0.0", compatibility.recommendedVersion, "Should recommend latest version")
        assertTrue(compatibility.migrationRequired, "Should indicate migration needed")
    }

    // Helper methods
    private fun createServiceMetadata(
        serviceName: String,
        dependencies: List<String>,
        isPublic: Boolean = true
    ): DependencyServiceInfo {
        return DependencyServiceInfo(
            serviceName = serviceName,
            dependencies = dependencies,
            moduleName = "test-module",
            packageName = "com.example.test",
            isPublic = isPublic
        )
    }

    private fun createVersionedServiceMetadata(
        serviceName: String,
        version: String,
        dependencies: List<String>
    ): VersionedServiceInfo {
        return VersionedServiceInfo(
            serviceName = serviceName,
            version = version,
            dependencies = dependencies,
            moduleName = "test-module"
        )
    }
}
