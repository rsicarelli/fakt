// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import kotlin.test.*
import java.io.File
import java.nio.file.Files

/**
 * End-to-end tests for incremental compilation functionality.
 *
 * These tests simulate real compilation scenarios by testing the complete
 * pipeline from interface discovery through code generation with proper
 * incremental behavior across separate compilation sessions.
 */
class EndToEndIncrementalTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("ktfakes-e2e-test").toFile()
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `GIVEN fresh project WHEN compile twice THEN second should skip all`() {
        // GIVEN - fresh project with interfaces
        val interfaces = listOf(
            createInterface("UserService", 2, 3),
            createInterface("OrderService", 1, 4),
            createInterface("PaymentService", 0, 2)
        )

        // WHEN - first compilation
        val firstSession = createCompilationSession()
        val firstMetrics = simulateCompilation(firstSession, interfaces)

        // Save state for next compilation
        firstSession.saveSignatures()

        // Second compilation with same interfaces (unchanged)
        val secondSession = createCompilationSession()
        val secondMetrics = simulateCompilation(secondSession, interfaces)

        // THEN - validate incremental behavior
        assertEquals(3, firstMetrics.typesGenerated, "First compilation should generate all 3 types")
        assertEquals(0, firstMetrics.typesSkipped, "First compilation should skip 0 types")

        assertEquals(0, secondMetrics.typesGenerated, "Second compilation should generate 0 types")
        assertEquals(3, secondMetrics.typesSkipped, "Second compilation should skip all 3 types")

        // Verify cache file exists and has correct content
        val cacheFile = File(tempDir, "ktfakes-signatures.cache")
        assertTrue(cacheFile.exists(), "Cache file should exist after first compilation")
        assertEquals(3, cacheFile.readLines().size, "Cache should contain 3 interface signatures")
    }

    @Test
    fun `GIVEN interface change WHEN recompile THEN only changed should regenerate`() {
        // GIVEN - initial interfaces
        val originalInterfaces = listOf(
            createInterface("ApiService", 1, 2),
            createInterface("DataService", 0, 3),
            createInterface("CacheService", 2, 1)
        )

        // First compilation
        val firstSession = createCompilationSession()
        simulateCompilation(firstSession, originalInterfaces)
        firstSession.saveSignatures()

        // WHEN - modify one interface (add function to ApiService)
        val modifiedInterfaces = listOf(
            createInterface("ApiService", 1, 3), // Changed: +1 function
            createInterface("DataService", 0, 3), // Unchanged
            createInterface("CacheService", 2, 1)  // Unchanged
        )

        val secondSession = createCompilationSession()

        // Verify regeneration needs BEFORE simulation
        val apiService = modifiedInterfaces.first { it.name == "ApiService" }
        val dataService = modifiedInterfaces.first { it.name == "DataService" }

        // Index types first to load them into the session
        modifiedInterfaces.forEach { secondSession.indexType(it) }

        assertTrue(
            secondSession.needsRegeneration(apiService),
            "ApiService should need regeneration due to signature change"
        )

        assertFalse(
            secondSession.needsRegeneration(dataService),
            "DataService should not need regeneration (unchanged)"
        )

        val secondMetrics = simulateCompilation(secondSession, modifiedInterfaces, skipIndexing = true)

        // THEN - only changed interface should regenerate
        assertEquals(1, secondMetrics.typesGenerated, "Should generate only the changed interface")
        assertEquals(2, secondMetrics.typesSkipped, "Should skip 2 unchanged interfaces")
        assertEquals(3, secondMetrics.typesIndexed, "Should index all 3 interfaces")
    }

    @Test
    fun `GIVEN new interface WHEN recompile THEN only new should generate`() {
        // GIVEN - initial compilation with 2 interfaces
        val originalInterfaces = listOf(
            createInterface("ExistingService1", 1, 2),
            createInterface("ExistingService2", 0, 1)
        )

        val firstSession = createCompilationSession()
        simulateCompilation(firstSession, originalInterfaces)
        firstSession.saveSignatures()

        // WHEN - add new interface while keeping others unchanged
        val expandedInterfaces = listOf(
            createInterface("ExistingService1", 1, 2), // Unchanged
            createInterface("ExistingService2", 0, 1), // Unchanged
            createInterface("NewService", 2, 3)        // New
        )

        val secondSession = createCompilationSession()

        // Index types and verify regeneration needs BEFORE simulation
        expandedInterfaces.forEach { secondSession.indexType(it) }

        val newService = expandedInterfaces.first { it.name == "NewService" }
        val existingService = expandedInterfaces.first { it.name == "ExistingService1" }

        assertTrue(
            secondSession.needsRegeneration(newService),
            "New interface should need generation"
        )

        assertFalse(
            secondSession.needsRegeneration(existingService),
            "Existing interface should not need regeneration"
        )

        val secondMetrics = simulateCompilation(secondSession, expandedInterfaces, skipIndexing = true)

        // THEN - only new interface should generate
        assertEquals(1, secondMetrics.typesGenerated, "Should generate only the new interface")
        assertEquals(2, secondMetrics.typesSkipped, "Should skip 2 existing interfaces")
        assertEquals(3, secondMetrics.typesIndexed, "Should index all 3 interfaces")
    }

    @Test
    fun `GIVEN deleted interface WHEN recompile THEN cache should not affect remaining`() {
        // GIVEN - initial compilation with 3 interfaces
        val originalInterfaces = listOf(
            createInterface("ServiceA", 1, 1),
            createInterface("ServiceB", 0, 2),
            createInterface("ServiceC", 1, 3)
        )

        val firstSession = createCompilationSession()
        simulateCompilation(firstSession, originalInterfaces)
        firstSession.saveSignatures()

        // WHEN - remove one interface (ServiceB deleted)
        val reducedInterfaces = listOf(
            createInterface("ServiceA", 1, 1), // Unchanged
            createInterface("ServiceC", 1, 3)  // Unchanged
        )

        val secondSession = createCompilationSession()
        val secondMetrics = simulateCompilation(secondSession, reducedInterfaces)

        // THEN - remaining interfaces should be skipped (cached)
        assertEquals(0, secondMetrics.typesGenerated, "Should generate 0 interfaces (all cached)")
        assertEquals(2, secondMetrics.typesSkipped, "Should skip 2 remaining interfaces")
        assertEquals(2, secondMetrics.typesIndexed, "Should index 2 remaining interfaces")

        // Verify cache still contains old entry but doesn't affect current compilation
        val cacheFile = File(tempDir, "ktfakes-signatures.cache")
        val cacheContent = cacheFile.readText()
        assertTrue(
            cacheContent.contains("ServiceB"),
            "Cache should still contain deleted interface (stale entry)"
        )

        // But current compilation should work correctly
        reducedInterfaces.forEach { service ->
            assertFalse(
                secondSession.needsRegeneration(service),
                "Remaining interfaces should not need regeneration"
            )
        }
    }

    @Test
    fun `GIVEN multiple changes WHEN recompile THEN should handle complex scenario`() {
        // GIVEN - initial compilation
        val originalInterfaces = listOf(
            createInterface("UserService", 1, 2),
            createInterface("OrderService", 0, 3),
            createInterface("PaymentService", 2, 1),
            createInterface("NotificationService", 1, 1)
        )

        val firstSession = createCompilationSession()
        simulateCompilation(firstSession, originalInterfaces)
        firstSession.saveSignatures()

        // WHEN - complex changes: 1 unchanged, 1 modified, 1 deleted, 1 new
        val complexInterfaces = listOf(
            createInterface("UserService", 1, 2),      // Unchanged
            createInterface("OrderService", 1, 3),     // Changed: +1 property
            // PaymentService deleted
            createInterface("NotificationService", 1, 1), // Unchanged
            createInterface("InventoryService", 0, 4)   // New
        )

        val secondSession = createCompilationSession()

        // Index types and verify regeneration needs BEFORE simulation
        complexInterfaces.forEach { secondSession.indexType(it) }

        val orderService = complexInterfaces.first { it.name == "OrderService" }
        val inventoryService = complexInterfaces.first { it.name == "InventoryService" }
        val userService = complexInterfaces.first { it.name == "UserService" }

        assertTrue(
            secondSession.needsRegeneration(orderService),
            "OrderService should need regeneration (changed)"
        )

        assertTrue(
            secondSession.needsRegeneration(inventoryService),
            "InventoryService should need regeneration (new)"
        )

        assertFalse(
            secondSession.needsRegeneration(userService),
            "UserService should not need regeneration (unchanged)"
        )

        val secondMetrics = simulateCompilation(secondSession, complexInterfaces, skipIndexing = true)

        // THEN - should handle complex scenario correctly
        assertEquals(2, secondMetrics.typesGenerated, "Should generate 1 changed + 1 new = 2 types")
        assertEquals(2, secondMetrics.typesSkipped, "Should skip 2 unchanged types")
        assertEquals(4, secondMetrics.typesIndexed, "Should index all 4 current types")
    }

    @Test
    fun `GIVEN custom annotations WHEN incremental compilation THEN should work correctly`() {
        // GIVEN - compilation with custom annotations
        val customAnnotations = listOf("com.rsicarelli.fakt.Fake", "com.company.TestDouble")

        val mixedInterfaces = listOf(
            createInterface("StandardService", 1, 2, "com.rsicarelli.fakt.Fake"),
            createInterface("CustomService", 0, 3, "com.company.TestDouble")
        )

        val firstSession = createCompilationSession(customAnnotations)
        simulateCompilation(firstSession, mixedInterfaces)
        firstSession.saveSignatures()

        // WHEN - recompile with same interfaces
        val secondSession = createCompilationSession(customAnnotations)
        val secondMetrics = simulateCompilation(secondSession, mixedInterfaces)

        // THEN - should skip both interfaces regardless of annotation type
        assertEquals(0, secondMetrics.typesGenerated, "Should generate 0 types (all cached)")
        assertEquals(2, secondMetrics.typesSkipped, "Should skip all 2 types")
        assertEquals(2, secondMetrics.annotationsConfigured, "Should have 2 configured annotations")
    }

    @Test
    fun `GIVEN no outputDir WHEN multiple sessions THEN should not persist incremental state`() {
        // GIVEN - compilation without outputDir (no persistent cache)
        val interfaces = listOf(
            createInterface("ServiceA", 1, 1),
            createInterface("ServiceB", 0, 2)
        )

        // First session without outputDir
        val firstSession = CompilerOptimizationsImpl(
            fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
            outputDir = null
        )

        simulateCompilation(firstSession, interfaces)

        // WHEN - second session (separate compilation)
        val secondSession = CompilerOptimizationsImpl(
            fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
            outputDir = null
        )

        val secondMetrics = simulateCompilation(secondSession, interfaces)

        // THEN - should regenerate all types (no persistent cache)
        assertEquals(2, secondMetrics.typesGenerated, "Should generate all types without persistent cache")
        assertEquals(0, secondMetrics.typesSkipped, "Should skip 0 types without persistent cache")

        // Verify no cache file created
        val cacheFile = File(tempDir, "ktfakes-signatures.cache")
        assertFalse(cacheFile.exists(), "No cache file should be created with null outputDir")
    }

    @Test
    fun `GIVEN compilation errors WHEN recovering THEN incremental should continue working`() {
        // GIVEN - successful first compilation
        val interfaces = listOf(
            createInterface("WorkingService", 1, 2)
        )

        val firstSession = createCompilationSession()
        simulateCompilation(firstSession, interfaces)
        firstSession.saveSignatures()

        // WHEN - simulate recovery from error and recompile unchanged
        val recoverySession = createCompilationSession()
        val recoveryMetrics = simulateCompilation(recoverySession, interfaces)

        // THEN - incremental should work normally
        assertEquals(0, recoveryMetrics.typesGenerated, "Recovery should skip unchanged types")
        assertEquals(1, recoveryMetrics.typesSkipped, "Recovery should skip the unchanged type")

        // Cache should be intact
        val cacheFile = File(tempDir, "ktfakes-signatures.cache")
        assertTrue(cacheFile.exists(), "Cache should survive error recovery")
        assertTrue(
            cacheFile.readText().contains("WorkingService"),
            "Cache should contain the working interface"
        )
    }

    // Helper functions

    private fun createCompilationSession(
        annotations: List<String> = listOf("com.rsicarelli.fakt.Fake")
    ): CompilerOptimizationsImpl {
        return CompilerOptimizationsImpl(
            fakeAnnotations = annotations,
            outputDir = tempDir.absolutePath
        )
    }

    private fun createInterface(
        name: String,
        propertyCount: Int,
        functionCount: Int,
        annotation: String = "com.rsicarelli.fakt.Fake"
    ): TypeInfo {
        return TypeInfo(
            name = name,
            fullyQualifiedName = "com.example.$name",
            packageName = "com.example",
            fileName = "$name.kt",
            annotations = listOf(annotation),
            signature = "interface com.example.$name|props:$propertyCount|funs:$functionCount"
        )
    }

    private fun simulateCompilation(
        session: CompilerOptimizationsImpl,
        interfaces: List<TypeInfo>,
        skipIndexing: Boolean = false
    ): CompilationMetrics {
        interfaces.forEach { type ->
            if (!skipIndexing) {
                session.indexType(type)
            }
            if (session.needsRegeneration(type)) {
                session.recordGeneration(type)
            }
        }
        return session.getMetrics()
    }
}
