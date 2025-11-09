// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.optimization

import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.types.TypeInfo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD tests for CompilerOptimizations - in-memory caching for KMP builds.
 *
 * **Test Strategy**:
 * 1. Test that first generation requires regeneration
 * 2. Test that subsequent generations skip regeneration (cache hit)
 * 3. Test that different interfaces are handled independently
 * 4. Test annotation configuration
 * 5. Test type indexing and lookup
 *
 * **Note on Signature Format**:
 * These tests use simplified structural signatures for readability:
 * - Test format: `"interface com.example.UserService|props:0|funs:2"`
 * - Production format: MD5 file hash (32-character hex: `"a3f8b7c9d1e2f5a6b8c0d3e5f7a9b1c3"`)
 *
 * Production code uses MD5 file hashing via `SignatureBuilder.buildSignature()`.
 * The cache system is format-agnostic (string equality only), so both work.
 * Structural format is only used in production as fallback when source file unavailable.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilerOptimizationsTest {
    @Test
    fun `GIVEN new interface WHEN checking regeneration THEN should return true`() =
        runTest {
            // GIVEN: Fresh optimization instance with new interface
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val typeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = "interface com.example.UserService|props:0|funs:2",
                )

            // WHEN: Checking if regeneration is needed
            val needsRegen = optimizations.needsRegeneration(typeInfo)

            // THEN: Should need regeneration (first time)
            assertTrue(needsRegen, "First generation should require regeneration")
        }

    @Test
    fun `GIVEN interface already generated WHEN checking regeneration THEN should return false`() =
        runTest {
            // GIVEN: Optimization instance with already generated interface
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val typeInfo =
                TypeInfo(
                    name = "PaymentService",
                    fullyQualifiedName = "com.example.PaymentService",
                    packageName = "com.example",
                    fileName = "PaymentService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = "interface com.example.PaymentService|props:1|funs:3",
                )

            // First generation
            optimizations.recordGeneration(typeInfo)

            // WHEN: Checking regeneration for same interface
            val needsRegen = optimizations.needsRegeneration(typeInfo)

            // THEN: Should skip regeneration (cached)
            assertFalse(needsRegen, "Already generated interface should skip regeneration")
        }

    @Test
    fun `GIVEN multiple targets processing same interface WHEN checking regeneration THEN first should generate rest should skip`() =
        runTest {
            // GIVEN: Multiple KMP targets (jvm, js, native) with same interface
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val interfaceSignature = "interface com.example.AuthService|props:2|funs:4"
            val typeInfo =
                TypeInfo(
                    name = "AuthService",
                    fullyQualifiedName = "com.example.AuthService",
                    packageName = "com.example",
                    fileName = "AuthService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = interfaceSignature,
                )

            // WHEN: First target (jvm) checks regeneration
            val jvmNeedsRegen = optimizations.needsRegeneration(typeInfo)
            optimizations.recordGeneration(typeInfo)

            // Second target (js) checks regeneration
            val jsNeedsRegen = optimizations.needsRegeneration(typeInfo)

            // Third target (native) checks regeneration
            val nativeNeedsRegen = optimizations.needsRegeneration(typeInfo)

            // THEN: Only first target should generate
            assertTrue(jvmNeedsRegen, "First target (jvm) should generate")
            assertFalse(jsNeedsRegen, "Second target (js) should skip (cached)")
            assertFalse(nativeNeedsRegen, "Third target (native) should skip (cached)")
        }

    @Test
    fun `GIVEN different interfaces WHEN checking regeneration THEN should handle independently`() =
        runTest {
            // GIVEN: Two different interfaces
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val typeInfo1 =
                TypeInfo(
                    name = "ServiceA",
                    fullyQualifiedName = "com.example.ServiceA",
                    packageName = "com.example",
                    fileName = "ServiceA.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = "interface com.example.ServiceA|props:1|funs:2",
                )
            val typeInfo2 =
                TypeInfo(
                    name = "ServiceB",
                    fullyQualifiedName = "com.example.ServiceB",
                    packageName = "com.example",
                    fileName = "ServiceB.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = "interface com.example.ServiceB|props:2|funs:3",
                )

            // WHEN: Recording generation for first interface only
            optimizations.recordGeneration(typeInfo1)

            val service1NeedsRegen = optimizations.needsRegeneration(typeInfo1)
            val service2NeedsRegen = optimizations.needsRegeneration(typeInfo2)

            // THEN: First should skip, second should generate
            assertFalse(service1NeedsRegen, "ServiceA should skip (cached)")
            assertTrue(service2NeedsRegen, "ServiceB should generate (not cached)")
        }

    @Test
    fun `GIVEN custom annotation WHEN checking configuration THEN should recognize configured annotation`() =
        runTest {
            // GIVEN: Optimizations configured with custom annotation
            val customAnnotation = "com.company.GenerateFake"
            val optimizations = CompilerOptimizations(fakeAnnotations = listOf(customAnnotation), logger = FaktLogger.quiet())

            // WHEN: Checking if annotation is configured
            val isConfigured = optimizations.isConfiguredFor(customAnnotation)
            val isDefaultConfigured = optimizations.isConfiguredFor("com.rsicarelli.fakt.Fake")

            // THEN: Should only recognize custom annotation
            assertTrue(isConfigured, "Custom annotation should be configured")
            assertFalse(isDefaultConfigured, "Default annotation should not be configured")
        }

    @Test
    fun `GIVEN indexed types WHEN finding by annotation THEN should return matching types`() =
        runTest {
            // GIVEN: Multiple indexed types with different annotations
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val fakeAnnotation = "com.rsicarelli.fakt.Fake"
            val customAnnotation = "com.company.Custom"

            val typeWithFake =
                TypeInfo(
                    name = "ServiceA",
                    fullyQualifiedName = "com.example.ServiceA",
                    packageName = "com.example",
                    fileName = "ServiceA.kt",
                    annotations = listOf(fakeAnnotation),
                    signature = "interface com.example.ServiceA|props:0|funs:1",
                )

            val typeWithCustom =
                TypeInfo(
                    name = "ServiceB",
                    fullyQualifiedName = "com.example.ServiceB",
                    packageName = "com.example",
                    fileName = "ServiceB.kt",
                    annotations = listOf(customAnnotation),
                    signature = "interface com.example.ServiceB|props:0|funs:2",
                )

            optimizations.indexType(typeWithFake)
            optimizations.indexType(typeWithCustom)

            // WHEN: Finding types by annotation
            val fakeTypes = optimizations.findTypesWithAnnotation(fakeAnnotation)
            val customTypes = optimizations.findTypesWithAnnotation(customAnnotation)

            // THEN: Should return correct types for each annotation
            assertEquals(1, fakeTypes.size, "Should find 1 type with @Fake")
            assertEquals("ServiceA", fakeTypes.first().name)

            assertEquals(1, customTypes.size, "Should find 1 type with @Custom")
            assertEquals("ServiceB", customTypes.first().name)
        }

    @Test
    fun `GIVEN interface with modified signature WHEN checking regeneration THEN should require regeneration`() =
        runTest {
            // GIVEN: Interface that was generated, then modified
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val originalTypeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = "interface com.example.UserService|props:1|funs:2",
                )

            val modifiedTypeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = "interface com.example.UserService|props:2|funs:3", // Changed!
                )

            // Record generation with original signature
            optimizations.recordGeneration(originalTypeInfo)

            // WHEN: Checking regeneration with modified signature
            val needsRegen = optimizations.needsRegeneration(modifiedTypeInfo)

            // THEN: Should require regeneration (signature changed)
            assertTrue(needsRegen, "Modified interface should require regeneration")
        }

    @Test
    fun `GIVEN default configuration WHEN creating optimizations THEN should use Fake annotation`() =
        runTest {
            // GIVEN: Default configuration
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())

            // WHEN: Checking for default annotation
            val isConfigured = optimizations.isConfiguredFor("com.rsicarelli.fakt.Fake")

            // THEN: Should be configured for default @Fake annotation
            assertTrue(isConfigured, "Default configuration should include @Fake annotation")
        }
}
