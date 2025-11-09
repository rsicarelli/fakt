// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.transform

import com.rsicarelli.fakt.compiler.fir.metadata.FirTypeParameterInfo
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for FirToIrTransformer type bound sanitization.
 *
 * **Critical Fix**: FIR's coneType.toString() produces "kotlin/Any?" which is
 * invalid Kotlin syntax and causes compilation failures. The sanitization
 * converts these to clean, valid Kotlin type notation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirToIrTransformerTest {
    // Create transformer instance using reflection to access private methods
    private val transformer = FirToIrTransformer()
    private val sanitizeMethod =
        transformer::class.java
            .getDeclaredMethod(
                "sanitizeTypeBound",
                String::class.java,
            ).apply { isAccessible = true }

    private fun sanitize(bound: String): String = sanitizeMethod.invoke(transformer, bound) as String

    @Test
    fun `GIVEN kotlin stdlib type WHEN sanitizing bound THEN should remove kotlin prefix`() {
        // GIVEN: Type bound from FIR with kotlin/ prefix
        val firBound = "kotlin/Any?"

        // WHEN: Sanitizing the bound
        val result = sanitize(firBound)

        // THEN: Should produce clean Kotlin syntax without package
        assertEquals("Any?", result, "kotlin/Any? should become Any?")
    }

    @Test
    fun `GIVEN kotlin Comparable WHEN sanitizing THEN should remove kotlin prefix`() {
        // GIVEN: Generic constraint with kotlin/ prefix
        val firBound = "kotlin/Comparable<T>"

        // WHEN: Sanitizing
        val result = sanitize(firBound)

        // THEN: Should be simplified
        assertEquals("Comparable<T>", result)
    }

    @Test
    fun `GIVEN kotlin collections type WHEN sanitizing THEN should remove full path`() {
        // GIVEN: Collection type with full kotlin/collections/ path
        val firBound = "kotlin/collections/List<T>"

        // WHEN: Sanitizing
        val result = sanitize(firBound)

        // THEN: Should remove kotlin.collections prefix
        assertEquals("List<T>", result)
    }

    @Test
    fun `GIVEN custom package type WHEN sanitizing THEN should use dots`() {
        // GIVEN: Custom package type with slashes
        val firBound = "com/example/MyInterface"

        // WHEN: Sanitizing
        val result = sanitize(firBound)

        // THEN: Should convert slashes to dots but keep package
        assertEquals("com.example.MyInterface", result)
    }

    @Test
    fun `GIVEN type parameter with single bound WHEN formatting THEN should format correctly`() {
        // GIVEN: Type parameter with kotlin/ bound
        val typeParam =
            FirTypeParameterInfo(
                name = "T",
                bounds = listOf("kotlin/Comparable<T>"),
            )
        val formatMethod =
            transformer::class.java
                .getDeclaredMethod(
                    "formatTypeParameter",
                    FirTypeParameterInfo::class.java,
                ).apply { isAccessible = true }

        // WHEN: Formatting
        val result = formatMethod.invoke(transformer, typeParam) as String

        // THEN: Should produce clean Kotlin syntax
        assertEquals("T : Comparable<T>", result)
    }
}
