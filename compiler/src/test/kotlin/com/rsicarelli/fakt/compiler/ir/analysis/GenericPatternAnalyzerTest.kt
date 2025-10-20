// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for GenericPatternAnalyzer with data structure validation.
 */
class GenericPatternAnalyzerTest {
    private val analyzer = GenericPatternAnalyzer()

    @Test
    fun `GIVEN GenericPatternAnalyzer WHEN creating instance THEN should initialize successfully`() {
        assertNotNull(analyzer)
    }

    @Test
    fun `GIVEN NoGenerics pattern WHEN getting analysis summary THEN should provide meaningful description`() {
        val noGenericsPattern = GenericPattern.NoGenerics
        val summary = GenericPatternAnalyzer.getAnalysisSummary(noGenericsPattern)

        assertTrue(summary.contains("No generic parameters"))
        assertTrue(summary.contains("simple generation"))
    }

    @Test
    fun `GIVEN ClassLevelGenerics pattern WHEN getting analysis summary THEN should describe generic type parameters`() {
        val classLevelPattern =
            GenericPattern.ClassLevelGenerics(
                typeParameters = emptyList(),
                constraints = emptyList(),
            )
        val summary = GenericPatternAnalyzer.getAnalysisSummary(classLevelPattern)

        assertTrue(summary.contains("Class-level generics"))
        assertTrue(summary.contains("type parameters"))
    }

    @Test
    fun `GIVEN MethodLevelGenerics pattern WHEN getting analysis summary THEN should describe generic methods`() {
        val methodLevelPattern =
            GenericPattern.MethodLevelGenerics(
                genericMethods = emptyList(),
            )
        val summary = GenericPatternAnalyzer.getAnalysisSummary(methodLevelPattern)

        assertTrue(summary.contains("Method-level generics"))
        assertTrue(summary.contains("generic methods"))
    }

    @Test
    fun `GIVEN MixedGenerics pattern WHEN getting analysis summary THEN should describe both class and method generics`() {
        val mixedPattern =
            GenericPattern.MixedGenerics(
                classTypeParameters = emptyList(),
                classConstraints = emptyList(),
                genericMethods = emptyList(),
            )
        val summary = GenericPatternAnalyzer.getAnalysisSummary(mixedPattern)

        assertTrue(summary.contains("Mixed generics"))
        assertTrue(summary.contains("class type parameters"))
        assertTrue(summary.contains("generic methods"))
    }
}
