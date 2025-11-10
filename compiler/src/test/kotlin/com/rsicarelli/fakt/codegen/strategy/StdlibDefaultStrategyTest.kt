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
 * Tests for StdlibDefaultStrategy.
 *
 * Verifies correct default value generation for Kotlin stdlib types.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StdlibDefaultStrategyTest {

    private val strategy = StdlibDefaultStrategy()

    @Test
    fun `GIVEN Unit type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Simple("Unit")

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN Unit type WHEN generating default THEN returns Unit`() {
        // GIVEN
        val type = CodeType.Simple("Unit")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        assertEquals("Unit", result.code)
    }

    @Test
    fun `GIVEN Flow type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic("Flow", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN Flow type WHEN generating default THEN returns emptyFlow call`() {
        // GIVEN
        val type = CodeType.Generic("Flow", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("emptyFlow", result.name)
        assertEquals("emptyFlow()", result.render())
    }

    @Test
    fun `GIVEN StateFlow type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic("StateFlow", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN StateFlow of Int WHEN generating default THEN returns MutableStateFlow with zero`() {
        // GIVEN
        val type = CodeType.Generic("StateFlow", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        val call = result
        assertEquals("MutableStateFlow", call.name)
        assertEquals(1, call.arguments.size)
        assertEquals("0", (call.arguments[0] as CodeExpression.NumberLiteral).value)
    }

    @Test
    fun `GIVEN StateFlow of String WHEN generating default THEN returns MutableStateFlow with empty string`() {
        // GIVEN
        val type = CodeType.Generic("StateFlow", listOf(CodeType.Simple("String")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        val call = result
        assertEquals("MutableStateFlow", call.name)
        assertEquals("", (call.arguments[0] as CodeExpression.StringLiteral).value)
    }

    @Test
    fun `GIVEN MutableStateFlow type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic("MutableStateFlow", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN MutableStateFlow type WHEN generating default THEN returns MutableStateFlow call`() {
        // GIVEN
        val type = CodeType.Generic("MutableStateFlow", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("MutableStateFlow", result.name)
    }

    @Test
    fun `GIVEN Result type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Generic("Result", listOf(CodeType.Simple("String")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN Result of String WHEN generating default THEN returns Result success with empty string`() {
        // GIVEN
        val type = CodeType.Generic("Result", listOf(CodeType.Simple("String")))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        val call = result
        assertEquals("Result.success", call.name)
        assertEquals("", (call.arguments[0] as CodeExpression.StringLiteral).value)
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
    fun `GIVEN collection type WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Generic("List", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }

    @Test
    fun `GIVEN nullable type WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("Unit"))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }
}
