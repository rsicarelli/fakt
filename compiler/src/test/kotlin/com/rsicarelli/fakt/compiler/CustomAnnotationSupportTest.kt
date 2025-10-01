// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for custom annotation support in the KtFakes compiler plugin.
 *
 * Validates that enterprises can configure their own annotations for fake generation,
 * providing better library ownership and protection against breaking changes.
 *
 * Supported scenarios include:
 * - Company-specific annotation names (e.g., `@TestDouble`, `@MockService`)
 * - Multiple annotation configuration
 * - Type discovery with custom annotations
 * - Incremental compilation with annotation changes
 * - Default configuration fallback behavior
 *
 * The compiler architecture supports flexible annotation configuration instead of
 * being hardcoded to `dev.rsicarelli.ktfake.Fake`.
 */
class CustomAnnotationSupportTest {

    @Test
    fun `GIVEN custom annotation configuration WHEN creating compiler THEN should accept any annotation`() {
        // GIVEN
        val customAnnotation = "com.company.testing.TestDouble"

        // WHEN
        val optimizations = CompilerOptimizations(
            fakeAnnotations = listOf(customAnnotation)
        )

        // THEN
        assertTrue(optimizations.isConfiguredFor(customAnnotation))
    }

    @Test
    fun `GIVEN multiple annotation configuration WHEN checking types THEN should support all annotations`() {
        // GIVEN
        val annotations = listOf(
            "com.company.TestDouble",
            "org.framework.Mock",
            "com.rsicarelli.fakt.Fake"
        )

        // WHEN
        val optimizations = CompilerOptimizations(
            fakeAnnotations = annotations
        )

        // THEN
        annotations.forEach { annotation ->
            assertTrue(optimizations.isConfiguredFor(annotation))
        }
    }

    @Test
    fun `GIVEN type discovery WHEN using custom annotation THEN should find annotated types`() {
        // GIVEN
        val customAnnotation = "com.company.TestDouble"
        val optimizations = CompilerOptimizations(
            fakeAnnotations = listOf(customAnnotation)
        )

        val mockType = TypeInfo(
            name = "UserService",
            fullyQualifiedName = "com.example.UserService",
            packageName = "com.example",
            fileName = "UserService.kt",
            annotations = listOf(customAnnotation),
            signature = "interface UserService { fun getUser(): String }"
        )

        // WHEN
        optimizations.indexType(mockType)
        val discoveredTypes = optimizations.findTypesWithAnnotation(customAnnotation)

        // THEN
        assertEquals(1, discoveredTypes.size)
        assertEquals("UserService", discoveredTypes.first().name)
    }

    @Test
    fun `GIVEN type with wrong annotation WHEN discovering THEN should not find type`() {
        // GIVEN
        val targetAnnotation = "com.company.TestDouble"
        val wrongAnnotation = "com.other.SomeOtherAnnotation"

        val optimizations = CompilerOptimizations(
            fakeAnnotations = listOf(targetAnnotation)
        )

        val mockType = TypeInfo(
            name = "UserService",
            fullyQualifiedName = "com.example.UserService",
            packageName = "com.example",
            fileName = "UserService.kt",
            annotations = listOf(wrongAnnotation), // Wrong annotation
            signature = "interface UserService { fun getUser(): String }"
        )

        // WHEN
        optimizations.indexType(mockType)
        val discoveredTypes = optimizations.findTypesWithAnnotation(targetAnnotation)

        // THEN
        assertEquals(0, discoveredTypes.size)
    }

    @Test
    fun `GIVEN type with multiple annotations WHEN discovering THEN should find if any match`() {
        // GIVEN
        val targetAnnotation = "com.company.TestDouble"
        val optimizations = CompilerOptimizations(
            fakeAnnotations = listOf(targetAnnotation)
        )

        val mockType = TypeInfo(
            name = "UserService",
            fullyQualifiedName = "com.example.UserService",
            packageName = "com.example",
            fileName = "UserService.kt",
            annotations = listOf(
                "javax.inject.Inject",
                targetAnnotation,  // Target annotation present
                "org.springframework.stereotype.Service"
            ),
            signature = "interface UserService { fun getUser(): String }"
        )

        // WHEN
        optimizations.indexType(mockType)
        val discoveredTypes = optimizations.findTypesWithAnnotation(targetAnnotation)

        // THEN
        assertEquals(1, discoveredTypes.size)
        assertEquals("UserService", discoveredTypes.first().name)
    }

    @Test
    fun `GIVEN incremental compilation WHEN type signature changes THEN should detect change regardless of annotation`() {
        // GIVEN
        val customAnnotation = "com.company.TestDouble"
        val optimizations = CompilerOptimizations(
            fakeAnnotations = listOf(customAnnotation)
        )

        val originalType = TypeInfo(
            name = "UserService",
            fullyQualifiedName = "com.example.UserService",
            packageName = "com.example",
            fileName = "UserService.kt",
            annotations = listOf(customAnnotation),
            signature = "interface UserService { fun getUser(): String }"
        )

        val changedType = originalType.copy(
            signature = "interface UserService { fun getUser(): String; fun setUser(user: String): Unit }"
        )

        // WHEN
        optimizations.recordGeneration(originalType)
        val needsRegeneration = optimizations.needsRegeneration(changedType)

        // THEN
        assertTrue(needsRegeneration)
    }

    @Test
    fun `GIVEN default configuration WHEN no annotations specified THEN should use standard Fake annotation`() {
        // GIVEN - No explicit configuration
        val optimizations = CompilerOptimizations()

        // WHEN & THEN
        assertTrue(optimizations.isConfiguredFor("com.rsicarelli.fakt.Fake"))
    }
}
