// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for incremental compilation metrics, specifically validating
 * the accuracy of `typesSkipped`, `typesGenerated`, and related metrics
 * in different compilation scenarios.
 */
class IncrementalMetricsTest {
    private lateinit var tempDir: File
    private lateinit var optimizations: CompilerOptimizationsImpl

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("ktfakes-metrics-test").toFile()
        optimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `GIVEN no previous cache WHEN first compilation THEN typesSkipped should be 0`() {
        // GIVEN - fresh compilation with multiple types
        val types =
            listOf(
                createTypeInfo("UserService", "interface UserService|props:1|funs:2"),
                createTypeInfo("OrderService", "interface OrderService|props:0|funs:3"),
                createTypeInfo("PaymentService", "interface PaymentService|props:2|funs:1"),
            )

        // WHEN - process all types (first compilation)
        types.forEach { type ->
            optimizations.indexType(type)
            assertTrue(optimizations.needsRegeneration(type), "First compilation should need generation for ${type.name}")
            optimizations.recordGeneration(type)
        }

        // THEN - get metrics
        val metrics = optimizations.getMetrics()

        assertEquals(3, metrics.typesIndexed, "Should index all 3 types")
        assertEquals(3, metrics.typesGenerated, "Should generate all 3 types in first compilation")
        assertEquals(0, metrics.typesSkipped, "Should skip 0 types in first compilation")
        assertEquals(1, metrics.annotationsConfigured, "Should have 1 configured annotation")
    }

    @Test
    fun `GIVEN unchanged interfaces WHEN recompilation THEN typesSkipped should equal total`() {
        // GIVEN - first compilation
        val types =
            listOf(
                createTypeInfo("UserService", "interface UserService|props:1|funs:2"),
                createTypeInfo("OrderService", "interface OrderService|props:0|funs:3"),
            )

        types.forEach { type ->
            optimizations.indexType(type)
            optimizations.recordGeneration(type)
        }
        optimizations.saveSignatures()

        // WHEN - second compilation with same types (unchanged)
        val newOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        types.forEach { type ->
            newOptimizations.indexType(type)
            assertFalse(newOptimizations.needsRegeneration(type), "Unchanged type should not need regeneration")
            // Don't call recordGeneration since we're skipping
        }

        // THEN - check metrics
        val metrics = newOptimizations.getMetrics()

        assertEquals(2, metrics.typesIndexed, "Should index all 2 types")
        assertEquals(0, metrics.typesGenerated, "Should generate 0 types when all unchanged")
        assertEquals(2, metrics.typesSkipped, "Should skip all 2 types when unchanged")
    }

    @Test
    fun `GIVEN 1 changed interface WHEN recompilation THEN typesSkipped should be N-1`() {
        // GIVEN - first compilation with 3 types
        val originalTypes =
            listOf(
                createTypeInfo("UserService", "interface UserService|props:1|funs:2"),
                createTypeInfo("OrderService", "interface OrderService|props:0|funs:3"),
                createTypeInfo("PaymentService", "interface PaymentService|props:2|funs:1"),
            )

        originalTypes.forEach { type ->
            optimizations.indexType(type)
            optimizations.recordGeneration(type)
        }
        optimizations.saveSignatures()

        // WHEN - second compilation with 1 changed type
        val newOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        val changedTypes =
            listOf(
                createTypeInfo("UserService", "interface UserService|props:1|funs:2"), // Unchanged
                createTypeInfo("OrderService", "interface OrderService|props:0|funs:4"), // Changed: +1 function
                createTypeInfo("PaymentService", "interface PaymentService|props:2|funs:1"), // Unchanged
            )

        var generated = 0
        var skipped = 0

        changedTypes.forEach { type ->
            newOptimizations.indexType(type)
            if (newOptimizations.needsRegeneration(type)) {
                newOptimizations.recordGeneration(type)
                generated++
            } else {
                skipped++
            }
        }

        // THEN - validate the counts match metrics
        assertEquals(1, generated, "Should generate exactly 1 changed type")
        assertEquals(2, skipped, "Should skip exactly 2 unchanged types")

        val metrics = newOptimizations.getMetrics()
        assertEquals(3, metrics.typesIndexed, "Should index all 3 types")
        assertEquals(1, metrics.typesGenerated, "Should generate 1 changed type")
        assertEquals(2, metrics.typesSkipped, "Should skip 2 unchanged types")
    }

    @Test
    fun `GIVEN mixed changed and new interfaces WHEN recompilation THEN metrics should be accurate`() {
        // GIVEN - first compilation with 2 types
        val originalTypes =
            listOf(
                createTypeInfo("UserService", "interface UserService|props:1|funs:2"),
                createTypeInfo("OrderService", "interface OrderService|props:0|funs:3"),
            )

        originalTypes.forEach { type ->
            optimizations.indexType(type)
            optimizations.recordGeneration(type)
        }
        optimizations.saveSignatures()

        // WHEN - second compilation with 1 unchanged, 1 changed, 1 new
        val newOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        val secondCompilationTypes =
            listOf(
                createTypeInfo("UserService", "interface UserService|props:1|funs:2"), // Unchanged
                createTypeInfo("OrderService", "interface OrderService|props:1|funs:3"), // Changed: +1 property
                createTypeInfo("PaymentService", "interface PaymentService|props:0|funs:1"), // New
            )

        var generated = 0
        var skipped = 0

        secondCompilationTypes.forEach { type ->
            newOptimizations.indexType(type)
            if (newOptimizations.needsRegeneration(type)) {
                newOptimizations.recordGeneration(type)
                generated++
            } else {
                skipped++
            }
        }

        // THEN - validate mixed scenario
        assertEquals(2, generated, "Should generate 1 changed + 1 new = 2 types")
        assertEquals(1, skipped, "Should skip 1 unchanged type")

        val metrics = newOptimizations.getMetrics()
        assertEquals(3, metrics.typesIndexed, "Should index all 3 types")
        assertEquals(2, metrics.typesGenerated, "Should generate 2 types (1 changed + 1 new)")
        assertEquals(1, metrics.typesSkipped, "Should skip 1 unchanged type")
    }

    @Test
    fun `GIVEN compilation metrics WHEN calculating skipped THEN should match manual count`() {
        // GIVEN - setup with multiple types in different states
        val types =
            listOf(
                createTypeInfo("Service1", "interface Service1|props:1|funs:1"),
                createTypeInfo("Service2", "interface Service2|props:0|funs:2"),
                createTypeInfo("Service3", "interface Service3|props:2|funs:0"),
                createTypeInfo("Service4", "interface Service4|props:1|funs:3"),
                createTypeInfo("Service5", "interface Service5|props:0|funs:1"),
            )

        // First compilation
        types.forEach { type ->
            optimizations.indexType(type)
            optimizations.recordGeneration(type)
        }
        optimizations.saveSignatures()

        // WHEN - second compilation with mixed changes
        val newOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        val changedTypes =
            listOf(
                createTypeInfo("Service1", "interface Service1|props:1|funs:1"), // Unchanged
                createTypeInfo("Service2", "interface Service2|props:0|funs:3"), // Changed
                createTypeInfo("Service3", "interface Service3|props:2|funs:0"), // Unchanged
                createTypeInfo("Service4", "interface Service4|props:2|funs:3"), // Changed
                createTypeInfo("Service5", "interface Service5|props:0|funs:1"), // Unchanged
            )

        var manualGenerated = 0
        var manualSkipped = 0

        changedTypes.forEach { type ->
            newOptimizations.indexType(type)
            if (newOptimizations.needsRegeneration(type)) {
                newOptimizations.recordGeneration(type)
                manualGenerated++
            } else {
                manualSkipped++
            }
        }

        // THEN - metrics should match manual counts
        val metrics = newOptimizations.getMetrics()

        assertEquals(manualGenerated, metrics.typesGenerated, "Metrics generated should match manual count")
        assertEquals(manualSkipped, metrics.typesSkipped, "Metrics skipped should match manual count")
        assertEquals(5, metrics.typesIndexed, "Should index all 5 types")
        assertEquals(5, manualGenerated + manualSkipped, "Manual counts should sum to total")
        assertEquals(5, metrics.typesGenerated + metrics.typesSkipped, "Metrics should sum to total")
    }

    @Test
    fun `GIVEN no outputDir WHEN compiling THEN should not track skipped metrics persistently`() {
        // GIVEN - optimization without persistent cache
        val noCacheOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = null,
            )

        val types =
            listOf(
                createTypeInfo("Service1", "interface Service1|props:1|funs:1"),
                createTypeInfo("Service2", "interface Service2|props:0|funs:2"),
            )

        // WHEN - first compilation
        types.forEach { type ->
            noCacheOptimizations.indexType(type)
            noCacheOptimizations.recordGeneration(type)
        }

        val firstMetrics = noCacheOptimizations.getMetrics()

        // New session (simulating separate compilation)
        val newSessionOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = null,
            )

        types.forEach { type ->
            newSessionOptimizations.indexType(type)
            // Should need regeneration (no persistent cache)
            assertTrue(newSessionOptimizations.needsRegeneration(type))
            newSessionOptimizations.recordGeneration(type)
        }

        val secondMetrics = newSessionOptimizations.getMetrics()

        // THEN - both sessions should generate all types (no persistent skipping)
        assertEquals(0, firstMetrics.typesSkipped, "First session should skip 0 types")
        assertEquals(0, secondMetrics.typesSkipped, "Second session should skip 0 types without persistent cache")
        assertEquals(2, firstMetrics.typesGenerated, "First session should generate all types")
        assertEquals(2, secondMetrics.typesGenerated, "Second session should generate all types")
    }

    @Test
    fun `GIVEN metrics during compilation WHEN checking compilation time THEN should be reasonable`() {
        // GIVEN - types to process
        val types =
            (1..10).map { i ->
                createTypeInfo("Service$i", "interface Service$i|props:1|funs:$i")
            }

        val startTime = System.currentTimeMillis()

        // WHEN - process types
        types.forEach { type ->
            optimizations.indexType(type)
            optimizations.recordGeneration(type)
        }

        val endTime = System.currentTimeMillis()
        val actualTime = endTime - startTime

        // THEN - get metrics and validate timing
        val metrics = optimizations.getMetrics()

        assertTrue(metrics.compilationTimeMs > 0, "Compilation time should be positive")
        assertTrue(metrics.compilationTimeMs <= actualTime + 500, "Compilation time should be reasonable (within 500ms of actual)")
        assertEquals(10, metrics.typesGenerated, "Should generate all 10 types")
        assertEquals(0, metrics.typesSkipped, "Should skip 0 types in first compilation")
    }

    private fun createTypeInfo(
        name: String,
        signature: String,
    ) = TypeInfo(
        name = name,
        fullyQualifiedName = "com.example.$name",
        packageName = "com.example",
        fileName = "$name.kt",
        annotations = listOf("com.rsicarelli.fakt.Fake"),
        signature = signature,
    )
}
