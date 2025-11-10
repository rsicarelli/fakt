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
 * Tests for PrimitiveDefaultStrategy.
 *
 * Verifies correct default value generation for Kotlin primitive types.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrimitiveDefaultStrategyTest {

    private val strategy = PrimitiveDefaultStrategy()

    @Test
    fun `GIVEN String type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Simple("String")

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN String type WHEN generating default THEN returns empty string`() {
        // GIVEN
        val type = CodeType.Simple("String")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.StringLiteral)
        assertEquals("", result.value)
        assertEquals("\"\"", result.render())
    }

    @Test
    fun `GIVEN Int type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Simple("Int")

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN Int type WHEN generating default THEN returns zero`() {
        // GIVEN
        val type = CodeType.Simple("Int")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.NumberLiteral)
        assertEquals("0", result.value)
    }

    @Test
    fun `GIVEN Long type WHEN generating default THEN returns zero L`() {
        // GIVEN
        val type = CodeType.Simple("Long")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.NumberLiteral)
        assertEquals("0L", result.value)
    }

    @Test
    fun `GIVEN Boolean type WHEN generating default THEN returns false`() {
        // GIVEN
        val type = CodeType.Simple("Boolean")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        assertEquals("false", result.code)
    }

    @Test
    fun `GIVEN Double type WHEN generating default THEN returns zero point zero`() {
        // GIVEN
        val type = CodeType.Simple("Double")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.NumberLiteral)
        assertEquals("0.0", result.value)
    }

    @Test
    fun `GIVEN Float type WHEN generating default THEN returns zero point zero f`() {
        // GIVEN
        val type = CodeType.Simple("Float")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.NumberLiteral)
        assertEquals("0.0f", result.value)
    }

    @Test
    fun `GIVEN Short type WHEN generating default THEN returns zero`() {
        // GIVEN
        val type = CodeType.Simple("Short")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.NumberLiteral)
        assertEquals("0", result.value)
    }

    @Test
    fun `GIVEN Byte type WHEN generating default THEN returns zero`() {
        // GIVEN
        val type = CodeType.Simple("Byte")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.NumberLiteral)
        assertEquals("0", result.value)
    }

    @Test
    fun `GIVEN Char type WHEN generating default THEN returns space character`() {
        // GIVEN
        val type = CodeType.Simple("Char")

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        assertEquals("' '", result.code)
    }

    @Test
    fun `GIVEN custom type WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Simple("User")

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }

    @Test
    fun `GIVEN generic type WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Generic("List", listOf(CodeType.Simple("String")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }

    @Test
    fun `GIVEN nullable type WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("String"))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }
}
