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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for signature cache persistence functionality.
 *
 * Tests the critical incremental compilation cache system that persists
 * interface signatures between compilations to enable efficient skipping
 * of unchanged types.
 */
class SignatureCacheTest {
    private lateinit var tempDir: File
    private lateinit var optimizations: CompilerOptimizationsImpl

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("ktfakes-cache-test").toFile()
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
    fun `GIVEN cache file exists WHEN loading signatures THEN should restore previous state`() {
        // GIVEN - pre-create a cache file with known signatures
        val cacheFile = File(tempDir, "ktfakes-signatures.cache")
        val expectedSignatures =
            listOf(
                "com.example.UserService@UserService.kt=interface com.example.UserService|props:1|funs:3",
                "com.example.OrderService@OrderService.kt=interface com.example.OrderService|props:0|funs:2",
            )
        cacheFile.writeText(expectedSignatures.joinToString("\n"))

        // WHEN - create new optimization instance (should load cache)
        val newOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        // Create TypeInfo objects that match the cached signatures
        val userServiceType =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.UserService|props:1|funs:3",
            )

        val orderServiceType =
            TypeInfo(
                name = "OrderService",
                fullyQualifiedName = "com.example.OrderService",
                packageName = "com.example",
                fileName = "OrderService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.OrderService|props:0|funs:2",
            )

        // THEN - previously cached types should not need regeneration
        assertFalse(
            newOptimizations.needsRegeneration(userServiceType),
            "UserService should not need regeneration (cached signature matches)",
        )
        assertFalse(
            newOptimizations.needsRegeneration(orderServiceType),
            "OrderService should not need regeneration (cached signature matches)",
        )
    }

    @Test
    fun `GIVEN empty cache WHEN first compilation THEN should create new cache file`() {
        // GIVEN - no cache file exists
        val cacheFile = File(tempDir, "ktfakes-signatures.cache")
        assertFalse(cacheFile.exists(), "Cache file should not exist initially")

        // WHEN - process and record a type
        val typeInfo =
            TypeInfo(
                name = "TestService",
                fullyQualifiedName = "com.example.TestService",
                packageName = "com.example",
                fileName = "TestService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.TestService|props:1|funs:2",
            )

        optimizations.indexType(typeInfo)
        optimizations.recordGeneration(typeInfo)
        optimizations.saveSignatures()

        // THEN - cache file should be created with correct content
        assertTrue(cacheFile.exists(), "Cache file should be created")
        val cacheContent = cacheFile.readText()
        assertTrue(
            cacheContent.contains("com.example.TestService@TestService.kt=interface com.example.TestService|props:1|funs:2"),
            "Cache should contain the recorded signature",
        )
    }

    @Test
    fun `GIVEN signatures saved WHEN loading again THEN should match exactly`() {
        // GIVEN - multiple types with different signatures
        val types =
            listOf(
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = "interface com.example.UserService|props:2|funs:5",
                ),
                TypeInfo(
                    name = "PaymentService",
                    fullyQualifiedName = "com.payment.PaymentService",
                    packageName = "com.payment",
                    fileName = "PaymentService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = "interface com.payment.PaymentService|props:0|funs:3",
                ),
                TypeInfo(
                    name = "CustomService",
                    fullyQualifiedName = "com.custom.CustomService",
                    packageName = "com.custom",
                    fileName = "CustomService.kt",
                    annotations = listOf("com.custom.TestDouble"),
                    signature = "interface com.custom.CustomService|props:1|funs:1",
                ),
            )

        // Record all types and save
        types.forEach { type ->
            optimizations.indexType(type)
            optimizations.recordGeneration(type)
        }
        optimizations.saveSignatures()

        // WHEN - create new instance and load cache
        val newOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake", "com.custom.TestDouble"),
                outputDir = tempDir.absolutePath,
            )

        // THEN - all types should match their cached signatures (no regeneration needed)
        types.forEach { type ->
            assertFalse(
                newOptimizations.needsRegeneration(type),
                "Type ${type.name} should not need regeneration (exact signature match)",
            )
        }
    }

    @Test
    fun `GIVEN changed signature WHEN checking regeneration THEN should detect change`() {
        // GIVEN - save a type with original signature
        val originalType =
            TypeInfo(
                name = "ApiService",
                fullyQualifiedName = "com.example.ApiService",
                packageName = "com.example",
                fileName = "ApiService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.ApiService|props:1|funs:2",
            )

        optimizations.indexType(originalType)
        optimizations.recordGeneration(originalType)
        optimizations.saveSignatures()

        // WHEN - create new instance and check type with changed signature
        val newOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        val changedType =
            originalType.copy(
                signature = "interface com.example.ApiService|props:1|funs:3", // Added function
            )

        // THEN - should detect change and need regeneration
        assertTrue(
            newOptimizations.needsRegeneration(changedType),
            "Changed signature should trigger regeneration",
        )
    }

    @Test
    fun `GIVEN invalid cache file WHEN loading THEN should handle gracefully`() {
        // GIVEN - create invalid cache file
        val cacheFile = File(tempDir, "ktfakes-signatures.cache")
        cacheFile.writeText("invalid-format-line\nno-equals-sign\n=empty-key")

        // WHEN - create new instance (should load gracefully)
        val newOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        // THEN - should not crash and treat all types as new
        val testType =
            TypeInfo(
                name = "TestService",
                fullyQualifiedName = "com.example.TestService",
                packageName = "com.example",
                fileName = "TestService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.TestService|props:1|funs:1",
            )

        assertTrue(
            newOptimizations.needsRegeneration(testType),
            "Should treat as new type when cache is invalid",
        )
    }

    @Test
    fun `GIVEN outputDir null WHEN saving signatures THEN should not crash`() {
        // GIVEN - optimization with null outputDir
        val nullDirOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = null,
            )

        val testType =
            TypeInfo(
                name = "TestService",
                fullyQualifiedName = "com.example.TestService",
                packageName = "com.example",
                fileName = "TestService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.TestService|props:1|funs:1",
            )

        // WHEN - operations should not crash with null outputDir
        nullDirOptimizations.indexType(testType)

        // First check - should need generation (new type)
        assertTrue(
            nullDirOptimizations.needsRegeneration(testType),
            "New type should need generation initially",
        )

        nullDirOptimizations.recordGeneration(testType)
        nullDirOptimizations.saveSignatures() // Should handle null gracefully

        // THEN - after recording, in the same session, should not need regeneration
        // (in-memory cache still works, just no persistence)
        assertFalse(
            nullDirOptimizations.needsRegeneration(testType),
            "Should not need regeneration in same session after recording",
        )

        // THEN - but a new session should not have persistent cache
        val newSessionOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = null,
            )

        assertTrue(
            newSessionOptimizations.needsRegeneration(testType),
            "New session without outputDir should treat as new (no persistent cache)",
        )
    }

    @Test
    fun `GIVEN new type WHEN checking regeneration THEN should need generation`() {
        // GIVEN - empty cache
        // (setup already creates clean state)

        // WHEN - check completely new type
        val newType =
            TypeInfo(
                name = "BrandNewService",
                fullyQualifiedName = "com.example.BrandNewService",
                packageName = "com.example",
                fileName = "BrandNewService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.BrandNewService|props:0|funs:1",
            )

        // THEN - should need generation (no cache entry exists)
        assertTrue(
            optimizations.needsRegeneration(newType),
            "New type should always need generation",
        )
    }

    @Test
    fun `GIVEN multiple compilations WHEN saving between sessions THEN cache should accumulate correctly`() {
        // GIVEN - first compilation session
        val firstType =
            TypeInfo(
                name = "FirstService",
                fullyQualifiedName = "com.example.FirstService",
                packageName = "com.example",
                fileName = "FirstService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.FirstService|props:1|funs:1",
            )

        optimizations.indexType(firstType)
        optimizations.recordGeneration(firstType)
        optimizations.saveSignatures()

        // WHEN - second compilation session with additional type
        val secondOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        val secondType =
            TypeInfo(
                name = "SecondService",
                fullyQualifiedName = "com.example.SecondService",
                packageName = "com.example",
                fileName = "SecondService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface com.example.SecondService|props:0|funs:2",
            )

        secondOptimizations.indexType(secondType)
        secondOptimizations.recordGeneration(secondType)
        secondOptimizations.saveSignatures()

        // THEN - third session should see both types in cache
        val thirdOptimizations =
            CompilerOptimizationsImpl(
                fakeAnnotations = listOf("com.rsicarelli.fakt.Fake"),
                outputDir = tempDir.absolutePath,
            )

        assertFalse(
            thirdOptimizations.needsRegeneration(firstType),
            "First type should be cached from first session",
        )
        assertFalse(
            thirdOptimizations.needsRegeneration(secondType),
            "Second type should be cached from second session",
        )
    }
}
