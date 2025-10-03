// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.optimization

import com.rsicarelli.fakt.compiler.optimization.IncrementalCompiler
import com.rsicarelli.fakt.compiler.types.TypeInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for [IncrementalCompiler] implementation.
 *
 * Validates all optimization capabilities including:
 * - Custom annotation configuration and detection
 * - Type indexing and efficient lookup
 * - Incremental compilation with change detection
 * - Performance characteristics for large-scale projects
 *
 * Uses TDD approach with GIVEN-WHEN-THEN structure for all scenarios.
 */
class CompilerOptimizationsTest {
    @Test
    fun `GIVEN default configuration WHEN creating optimizations THEN should support standard Fake annotation`() {
        // GIVEN - Default configuration
        val optimizations = IncrementalCompiler(listOf("com.rsicarelli.fakt.Fake"))

        // WHEN & THEN
        assertTrue(optimizations.isConfiguredFor("com.rsicarelli.fakt.Fake"))
        assertFalse(optimizations.isConfiguredFor("com.company.TestDouble"))
    }

    @Test
    fun `GIVEN custom annotation configuration WHEN creating optimizations THEN should support only configured annotations`() {
        // GIVEN
        val customAnnotations =
            listOf(
                "com.company.TestDouble",
                "com.enterprise.MockService",
            )

        // WHEN
        val optimizations = IncrementalCompiler(customAnnotations)

        // THEN
        assertTrue(optimizations.isConfiguredFor("com.company.TestDouble"))
        assertTrue(optimizations.isConfiguredFor("com.enterprise.MockService"))
        assertFalse(optimizations.isConfiguredFor("com.rsicarelli.fakt.Fake"))
        assertFalse(optimizations.isConfiguredFor("org.unknown.Annotation"))
    }

    @Test
    fun `GIVEN multiple annotations WHEN configuring optimizations THEN should support all provided annotations`() {
        // GIVEN
        val annotations =
            listOf(
                "com.company.TestDouble",
                "org.framework.Mock",
                "com.rsicarelli.fakt.Fake",
                "com.enterprise.FakeService",
            )

        // WHEN
        val optimizations = IncrementalCompiler(annotations)

        // THEN
        annotations.forEach { annotation ->
            assertTrue(optimizations.isConfiguredFor(annotation), "Should support $annotation")
        }
    }

    @Test
    fun `GIVEN empty annotation list WHEN creating optimizations THEN should not support any annotations`() {
        // GIVEN
        val emptyAnnotations = emptyList<String>()

        // WHEN
        val optimizations = IncrementalCompiler(emptyAnnotations)

        // THEN
        assertFalse(optimizations.isConfiguredFor("com.rsicarelli.fakt.Fake"))
        assertFalse(optimizations.isConfiguredFor("com.company.TestDouble"))
    }

    @Test
    fun `GIVEN type with target annotation WHEN indexing and searching THEN should find the type`() {
        // GIVEN
        val targetAnnotation = "com.company.TestDouble"
        val optimizations = IncrementalCompiler(listOf(targetAnnotation))

        val userServiceType =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations = listOf(targetAnnotation),
                signature = "interface UserService { fun getUser(): String }",
            )

        // WHEN
        optimizations.indexType(userServiceType)
        val foundTypes = optimizations.findTypesWithAnnotation(targetAnnotation)

        // THEN
        assertEquals(1, foundTypes.size)
        assertEquals("UserService", foundTypes.first().name)
        assertEquals("com.example.UserService", foundTypes.first().fullyQualifiedName)
    }

    @Test
    fun `GIVEN type with wrong annotation WHEN searching for target annotation THEN should not find the type`() {
        // GIVEN
        val targetAnnotation = "com.company.TestDouble"
        val wrongAnnotation = "com.other.DifferentAnnotation"
        val optimizations = IncrementalCompiler(listOf(targetAnnotation))

        val userServiceType =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations = listOf(wrongAnnotation),
                signature = "interface UserService { fun getUser(): String }",
            )

        // WHEN
        optimizations.indexType(userServiceType)
        val foundTypes = optimizations.findTypesWithAnnotation(targetAnnotation)

        // THEN
        assertEquals(0, foundTypes.size)
    }

    @Test
    fun `GIVEN type with multiple annotations WHEN searching THEN should find if any annotation matches`() {
        // GIVEN
        val targetAnnotation = "com.company.TestDouble"
        val optimizations = IncrementalCompiler(listOf(targetAnnotation))

        val userServiceType =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations =
                    listOf(
                        "javax.inject.Inject",
                        targetAnnotation, // Target annotation present
                        "org.springframework.stereotype.Service",
                    ),
                signature = "interface UserService { fun getUser(): String }",
            )

        // WHEN
        optimizations.indexType(userServiceType)
        val foundTypes = optimizations.findTypesWithAnnotation(targetAnnotation)

        // THEN
        assertEquals(1, foundTypes.size)
        assertEquals("UserService", foundTypes.first().name)
    }

    @Test
    fun `GIVEN multiple types WHEN indexing and searching THEN should find only matching types`() {
        // GIVEN
        val targetAnnotation = "com.company.TestDouble"
        val otherAnnotation = "com.other.Mock"
        val optimizations = IncrementalCompiler(listOf(targetAnnotation))

        val userServiceType =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations = listOf(targetAnnotation),
                signature = "interface UserService { fun getUser(): String }",
            )

        val orderServiceType =
            TypeInfo(
                name = "OrderService",
                fullyQualifiedName = "com.example.OrderService",
                packageName = "com.example",
                fileName = "OrderService.kt",
                annotations = listOf(otherAnnotation),
                signature = "interface OrderService { fun getOrders(): List<Order> }",
            )

        val paymentServiceType =
            TypeInfo(
                name = "PaymentService",
                fullyQualifiedName = "com.example.PaymentService",
                packageName = "com.example",
                fileName = "PaymentService.kt",
                annotations = listOf(targetAnnotation),
                signature = "interface PaymentService { fun processPayment(): Boolean }",
            )

        // WHEN
        optimizations.indexType(userServiceType)
        optimizations.indexType(orderServiceType)
        optimizations.indexType(paymentServiceType)
        val foundTypes = optimizations.findTypesWithAnnotation(targetAnnotation)

        // THEN
        assertEquals(2, foundTypes.size)
        val typeNames = foundTypes.map { it.name }.toSet()
        assertTrue(typeNames.contains("UserService"))
        assertTrue(typeNames.contains("PaymentService"))
        assertFalse(typeNames.contains("OrderService"))
    }

    @Test
    fun `GIVEN new type WHEN checking regeneration THEN should return true`() {
        // GIVEN
        val optimizations = IncrementalCompiler(listOf("com.rsicarelli.fakt.Fake"))
        val newType =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface UserService { fun getUser(): String }",
            )

        // WHEN
        val needsRegeneration = optimizations.needsRegeneration(newType)

        // THEN
        assertTrue(needsRegeneration, "New type should need generation")
    }

    @Test
    fun `GIVEN generated type WHEN checking regeneration with same signature THEN should return false`() {
        // GIVEN
        val optimizations = IncrementalCompiler(listOf("com.rsicarelli.fakt.Fake"))
        val typeInfo =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface UserService { fun getUser(): String }",
            )

        // WHEN
        optimizations.recordGeneration(typeInfo)
        val needsRegeneration = optimizations.needsRegeneration(typeInfo)

        // THEN
        assertFalse(needsRegeneration, "Unchanged type should not need regeneration")
    }

    @Test
    fun `GIVEN generated type WHEN signature changes THEN should return true for regeneration`() {
        // GIVEN
        val optimizations = IncrementalCompiler(listOf("com.rsicarelli.fakt.Fake"))
        val originalType =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface UserService { fun getUser(): String }",
            )

        val changedType =
            originalType.copy(
                signature = "interface UserService { fun getUser(): String; fun setUser(user: String): Unit }",
            )

        // WHEN
        optimizations.recordGeneration(originalType)
        val needsRegeneration = optimizations.needsRegeneration(changedType)

        // THEN
        assertTrue(needsRegeneration, "Changed signature should need regeneration")
    }

    @Test
    fun `GIVEN multiple types with different signatures WHEN recording generation THEN should track each separately`() {
        // GIVEN
        val optimizations = IncrementalCompiler(listOf("com.rsicarelli.fakt.Fake"))

        val userServiceType =
            TypeInfo(
                name = "UserService",
                fullyQualifiedName = "com.example.UserService",
                packageName = "com.example",
                fileName = "UserService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface UserService { fun getUser(): String }",
            )

        val orderServiceType =
            TypeInfo(
                name = "OrderService",
                fullyQualifiedName = "com.example.OrderService",
                packageName = "com.example",
                fileName = "OrderService.kt",
                annotations = listOf("com.rsicarelli.fakt.Fake"),
                signature = "interface OrderService { fun getOrders(): List<Order> }",
            )

        // WHEN
        optimizations.recordGeneration(userServiceType)

        // THEN
        assertFalse(optimizations.needsRegeneration(userServiceType), "UserService should not need regeneration")
        assertTrue(optimizations.needsRegeneration(orderServiceType), "OrderService should need generation")

        // WHEN - Record OrderService generation
        optimizations.recordGeneration(orderServiceType)

        // THEN
        assertFalse(optimizations.needsRegeneration(userServiceType), "UserService should still not need regeneration")
        assertFalse(optimizations.needsRegeneration(orderServiceType), "OrderService should not need regeneration")
    }

    @Test
    fun `GIVEN no types indexed WHEN searching for annotation THEN should return empty list`() {
        // GIVEN
        val optimizations = IncrementalCompiler(listOf("com.company.TestDouble"))

        // WHEN
        val foundTypes = optimizations.findTypesWithAnnotation("com.company.TestDouble")

        // THEN
        assertEquals(0, foundTypes.size)
    }

    @Test
    fun `GIVEN large number of types WHEN searching THEN should maintain performance characteristics`() {
        // GIVEN
        val targetAnnotation = "com.company.TestDouble"
        val optimizations = IncrementalCompiler(listOf(targetAnnotation))

        // Index many types to test performance
        repeat(1000) { index ->
            val hasTargetAnnotation = index % 10 == 0 // Every 10th type has target annotation
            val annotations =
                if (hasTargetAnnotation) {
                    listOf(targetAnnotation)
                } else {
                    listOf("com.other.Annotation$index")
                }

            val typeInfo =
                TypeInfo(
                    name = "Service$index",
                    fullyQualifiedName = "com.example.Service$index",
                    packageName = "com.example",
                    fileName = "Service$index.kt",
                    annotations = annotations,
                    signature = "interface Service$index { fun method$index(): String }",
                )
            optimizations.indexType(typeInfo)
        }

        // WHEN
        val foundTypes = optimizations.findTypesWithAnnotation(targetAnnotation)

        // THEN
        assertEquals(100, foundTypes.size, "Should find exactly 100 types with target annotation")
        foundTypes.forEach { type ->
            assertTrue(type.annotations.contains(targetAnnotation), "Found type should have target annotation")
        }
    }
}
