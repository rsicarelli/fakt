// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType
import com.rsicarelli.fakt.codegen.renderer.render
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CollectionDefaultStrategy.
 *
 * Verifies correct default value generation for Kotlin collection types.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollectionDefaultStrategyTest {

    private val strategy = CollectionDefaultStrategy()

    @Test
    fun `GIVEN List type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic("List", listOf(CodeType.Simple("String")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN List type WHEN generating default THEN returns emptyList call`() {
        // GIVEN
        val type = CodeType.Generic("List", listOf(CodeType.Simple("String")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("emptyList", result.name)
        assertEquals("emptyList()", result.render())
    }

    @Test
    fun `GIVEN Set type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic("Set", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN Set type WHEN generating default THEN returns emptySet call`() {
        // GIVEN
        val type = CodeType.Generic("Set", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("emptySet", result.name)
    }

    @Test
    fun `GIVEN Map type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic(
            "Map",
            listOf(CodeType.Simple("String"), CodeType.Simple("Int"))
        )

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN Map type WHEN generating default THEN returns emptyMap call`() {
        // GIVEN
        val type = CodeType.Generic(
            "Map",
            listOf(CodeType.Simple("String"), CodeType.Simple("Int"))
        )

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("emptyMap", result.name)
    }

    @Test
    fun `GIVEN Array type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic("Array", listOf(CodeType.Simple("User")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN Array type WHEN generating default THEN returns emptyArray call`() {
        // GIVEN
        val type = CodeType.Generic("Array", listOf(CodeType.Simple("User")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("emptyArray", result.name)
    }

    @Test
    fun `GIVEN MutableList type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic("MutableList", listOf(CodeType.Simple("String")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN MutableList type WHEN generating default THEN returns mutableListOf call`() {
        // GIVEN
        val type = CodeType.Generic("MutableList", listOf(CodeType.Simple("String")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("mutableListOf", result.name)
    }

    @Test
    fun `GIVEN MutableSet type WHEN generating default THEN returns mutableSetOf call`() {
        // GIVEN
        val type = CodeType.Generic("MutableSet", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("mutableSetOf", result.name)
    }

    @Test
    fun `GIVEN MutableMap type WHEN generating default THEN returns mutableMapOf call`() {
        // GIVEN
        val type = CodeType.Generic(
            "MutableMap",
            listOf(CodeType.Simple("String"), CodeType.Simple("Int"))
        )

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("mutableMapOf", result.name)
    }

    @Test
    fun `GIVEN primitive type WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Simple("String")

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }

    @Test
    fun `GIVEN custom generic type WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Generic("StateFlow", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }

    @Test
    fun `GIVEN nullable collection WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Nullable(
            CodeType.Generic("List", listOf(CodeType.Simple("String")))
        )

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }
}
