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
 * Tests for NullableDefaultStrategy.
 *
 * Verifies correct default value generation for nullable types.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NullableDefaultStrategyTest {
    private val strategy = NullableDefaultStrategy()

    @Test
    fun `GIVEN nullable String type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("String"))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN nullable String type WHEN generating default THEN returns null`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("String"))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        assertEquals("null", result.code)
        assertEquals("null", result.render())
    }

    @Test
    fun `GIVEN nullable Int type WHEN checking support THEN returns true`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("Int"))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN nullable Int type WHEN generating default THEN returns null`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("Int"))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        assertEquals("null", result.code)
    }

    @Test
    fun `GIVEN nullable User type WHEN generating default THEN returns null`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("User"))

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        assertEquals("null", result.code)
    }

    @Test
    fun `GIVEN nullable generic type WHEN checking support THEN returns true`() {
        // GIVEN
        val type =
            CodeType.Nullable(
                CodeType.Generic("List", listOf(CodeType.Simple("String"))),
            )

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN nullable List type WHEN generating default THEN returns null`() {
        // GIVEN
        val type =
            CodeType.Nullable(
                CodeType.Generic("List", listOf(CodeType.Simple("String"))),
            )

        // WHEN
        val result = strategy.defaultValue(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        assertEquals("null", result.code)
    }

    @Test
    fun `GIVEN non-nullable type WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Simple("String")

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }

    @Test
    fun `GIVEN non-nullable generic WHEN checking support THEN returns false`() {
        // GIVEN
        val type = CodeType.Generic("List", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = strategy.supports(type)

        // THEN
        assertFalse(result)
    }
}
