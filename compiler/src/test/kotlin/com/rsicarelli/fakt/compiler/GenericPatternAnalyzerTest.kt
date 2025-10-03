// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for GenericPatternAnalyzer focusing on testable functionality.
 * Full IR integration tests will be added when integrated with the main compiler.
 */
class GenericPatternAnalyzerTest {
    private val analyzer = GenericPatternAnalyzer()

    @Test
    fun `GIVEN GenericPatternAnalyzer WHEN creating instance THEN should initialize successfully`() {
        assertNotNull(analyzer)
    }

    @Test
    fun `GIVEN GenericPatternAnalyzer WHEN detecting transformation patterns THEN should identify common transformations`() {
        val patterns = analyzer.detectCommonTransformations()

        assertTrue(patterns.isNotEmpty(), "Should detect common transformation patterns")

        // Verify some expected patterns exist
        assertTrue(patterns.any { it.inputType == "User" && it.outputType == "UserDto" })
        assertTrue(patterns.any { it.inputType == "T" && it.outputType == "Result<T>" })
        assertTrue(patterns.any { it.inputType == "Order" && it.outputType == "OrderSummary" })
        assertTrue(patterns.any { it.inputType == "Product" && it.outputType == "ProductDto" })
    }

    @Test
    fun `GIVEN GenericPatternAnalyzer WHEN detecting contextual types from interface THEN should access detection method`() {
        // Test contextual type detection using reflection
        val analyzerClass = analyzer::class.java
        val detectContextualTypesMethod =
            analyzerClass.getDeclaredMethod(
                "detectContextualTypes",
                org.jetbrains.kotlin.ir.declarations.IrClass::class.java,
            )
        detectContextualTypesMethod.isAccessible = true

        // For now, we'll test the logic without full IR mocks
        // These will be expanded when we have proper IR test infrastructure
    }

    @Test
    fun `GIVEN GenericPatternAnalyzer WHEN detecting common types THEN should identify standard Kotlin types`() {
        val commonTypes = analyzer.detectCommonTypes()

        assertTrue(commonTypes.isNotEmpty(), "Should detect common types")
        assertTrue(commonTypes.contains("kotlin.String"))
        assertTrue(commonTypes.contains("kotlin.Int"))
        assertTrue(commonTypes.contains("kotlin.Boolean"))
        assertTrue(commonTypes.contains("User"))
        assertTrue(commonTypes.contains("Order"))
    }

    @Test
    fun `GIVEN NoGenerics pattern WHEN getting analysis summary THEN should provide meaningful description`() {
        val noGenericsPattern = GenericPattern.NoGenerics
        val summary = analyzer.getAnalysisSummary(noGenericsPattern)

        assertTrue(summary.contains("No generic parameters"))
        assertTrue(summary.contains("simple generation"))
    }

    @Test
    fun `GIVEN ClassLevelGenerics pattern WHEN getting analysis summary THEN should describe generic type parameters`() {
        val classLevelPattern =
            GenericPattern.ClassLevelGenerics(
                typeParameters = emptyList(), // In real usage, would have actual IrTypeParameters
                constraints = emptyList(),
            )
        val summary = analyzer.getAnalysisSummary(classLevelPattern)

        assertTrue(summary.contains("Class-level generics"))
        assertTrue(summary.contains("type parameters"))
    }

    @Test
    fun `GIVEN MethodLevelGenerics pattern WHEN getting analysis summary THEN should include detected types and patterns`() {
        val methodLevelPattern =
            GenericPattern.MethodLevelGenerics(
                genericMethods = emptyList(),
                detectedTypes = setOf("User", "Order"),
                transformationPatterns = listOf(TransformationPattern("User", "UserDto")),
            )
        val summary = analyzer.getAnalysisSummary(methodLevelPattern)

        assertTrue(summary.contains("Method-level generics"))
        assertTrue(summary.contains("detected types"))
        assertTrue(summary.contains("transformation patterns"))
    }

    @Test
    fun `GIVEN MixedGenerics pattern WHEN getting analysis summary THEN should describe both class and method generics`() {
        val mixedPattern =
            GenericPattern.MixedGenerics(
                classTypeParameters = emptyList(),
                classConstraints = emptyList(),
                genericMethods = emptyList(),
                detectedTypes = setOf("User"),
                transformationPatterns = emptyList(),
            )
        val summary = analyzer.getAnalysisSummary(mixedPattern)

        assertTrue(summary.contains("Mixed generics"))
        assertTrue(summary.contains("class type parameters"))
        assertTrue(summary.contains("generic methods"))
    }
}
