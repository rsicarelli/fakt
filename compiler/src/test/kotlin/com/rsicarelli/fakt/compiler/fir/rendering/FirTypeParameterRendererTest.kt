// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.rendering

import com.rsicarelli.fakt.compiler.fir.metadata.FirTypeParameterInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [formatTypeParameter] / [sanitizeTypeBound].
 *
 * **Critical Fix**: FIR's `ConeType.toString()` produces `"kotlin/Any?"` which is invalid Kotlin
 * syntax and would cause compilation failures if emitted verbatim. The sanitization converts these
 * to clean, valid Kotlin type notation. Shared by both the IR-phase (`FirToIrTransformer`) and
 * FIR-phase (`FirToFakeDeclarationTranslator`) emitters.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirTypeParameterRendererTest {

    @Test
    fun `GIVEN kotlin stdlib type WHEN sanitizing bound THEN should remove kotlin prefix`() {
        // GIVEN: Type bound from FIR with kotlin/ prefix
        val firBound = "kotlin/Any?"

        // WHEN: Sanitizing the bound
        val result = sanitizeTypeBound(firBound)

        // THEN: Should produce clean Kotlin syntax without package
        assertEquals("Any?", result, "kotlin/Any? should become Any?")
    }

    @Test
    fun `GIVEN kotlin Comparable WHEN sanitizing THEN should remove kotlin prefix`() {
        // GIVEN: Generic constraint with kotlin/ prefix
        val firBound = "kotlin/Comparable<T>"

        // WHEN: Sanitizing
        val result = sanitizeTypeBound(firBound)

        // THEN: Should be simplified
        assertEquals("Comparable<T>", result)
    }

    @Test
    fun `GIVEN kotlin collections type WHEN sanitizing THEN should remove full path`() {
        // GIVEN: Collection type with full kotlin/collections/ path
        val firBound = "kotlin/collections/List<T>"

        // WHEN: Sanitizing
        val result = sanitizeTypeBound(firBound)

        // THEN: Should remove kotlin.collections prefix
        assertEquals("List<T>", result)
    }

    @Test
    fun `GIVEN custom package type WHEN sanitizing THEN should use dots`() {
        // GIVEN: Custom package type with slashes
        val firBound = "com/example/MyInterface"

        // WHEN: Sanitizing
        val result = sanitizeTypeBound(firBound)

        // THEN: Should convert slashes to dots but keep package
        assertEquals("com.example.MyInterface", result)
    }

    @Test
    fun `GIVEN type parameter with single bound WHEN formatting THEN should format correctly`() {
        // GIVEN: Type parameter with kotlin/ bound
        val typeParam = FirTypeParameterInfo(name = "T", bounds = listOf("kotlin/Comparable<T>"))

        // WHEN: Formatting
        val result = formatTypeParameter(typeParam)

        // THEN: Should produce clean Kotlin syntax
        assertEquals("T : Comparable<T>", result)
    }

    @Test
    fun `GIVEN type parameter with no bounds WHEN formatting THEN should return bare name`() {
        // GIVEN: Unbounded type parameter
        val typeParam = FirTypeParameterInfo(name = "T", bounds = emptyList())

        // WHEN: Formatting
        val result = formatTypeParameter(typeParam)

        // THEN: No " : " suffix
        assertEquals("T", result)
    }

    @Test
    fun `GIVEN type parameter with multiple bounds WHEN formatting THEN should join with comma`() {
        // GIVEN: Multiple constraints (where clause)
        val typeParam =
            FirTypeParameterInfo(
                name = "V",
                bounds = listOf("kotlin/Comparable<V>", "com/example/Serializable"),
            )

        // WHEN: Formatting
        val result = formatTypeParameter(typeParam)

        // THEN: Bounds joined, each sanitized independently
        assertEquals("V : Comparable<V>, com.example.Serializable", result)
    }
}
