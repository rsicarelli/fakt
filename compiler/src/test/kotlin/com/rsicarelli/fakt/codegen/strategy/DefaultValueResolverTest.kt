// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for DefaultValueResolver.
 *
 * Verifies correct strategy composition and priority ordering.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultValueResolverTest {
    private val resolver = DefaultValueResolver()

    // ===========================================
    // Nullable Strategy (Highest Priority)
    // ===========================================

    @Test
    fun `GIVEN nullable String WHEN resolving THEN returns null`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("String"))

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        assertEquals("null", result.code)
    }

    @Test
    fun `GIVEN nullable Int WHEN resolving THEN returns null`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("Int"))

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertEquals("null", (result as CodeExpression.Raw).code)
    }

    @Test
    fun `GIVEN nullable List WHEN resolving THEN returns null not emptyList`() {
        // GIVEN
        val type =
            CodeType.Nullable(
                CodeType.Generic("List", listOf(CodeType.Simple("String"))),
            )

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        // Nullable wins over Collection strategy
        assertEquals("null", (result as CodeExpression.Raw).code)
    }

    // ===========================================
    // Primitive Strategy
    // ===========================================

    @Test
    fun `GIVEN String type WHEN resolving THEN returns empty string`() {
        // GIVEN
        val type = CodeType.Simple("String")

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.StringLiteral)
        assertEquals("", result.value)
    }

    @Test
    fun `GIVEN Int type WHEN resolving THEN returns zero`() {
        // GIVEN
        val type = CodeType.Simple("Int")

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.NumberLiteral)
        assertEquals("0", result.value)
    }

    @Test
    fun `GIVEN Boolean type WHEN resolving THEN returns false`() {
        // GIVEN
        val type = CodeType.Simple("Boolean")

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertEquals("false", (result as CodeExpression.Raw).code)
    }

    // ===========================================
    // Stdlib Strategy
    // ===========================================

    @Test
    fun `GIVEN Unit type WHEN resolving THEN returns Unit`() {
        // GIVEN
        val type = CodeType.Simple("Unit")

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertEquals("Unit", (result as CodeExpression.Raw).code)
    }

    @Test
    fun `GIVEN Flow type WHEN resolving THEN returns emptyFlow`() {
        // GIVEN
        val type = CodeType.Generic("Flow", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("emptyFlow", result.name)
    }

    @Test
    fun `GIVEN StateFlow of String WHEN resolving THEN returns MutableStateFlow with empty string`() {
        // GIVEN
        val type = CodeType.Generic("StateFlow", listOf(CodeType.Simple("String")))

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        val call = result
        assertEquals("MutableStateFlow", call.name)
        assertEquals("", (call.arguments[0] as CodeExpression.StringLiteral).value)
    }

    @Test
    fun `GIVEN Result of Int WHEN resolving THEN returns Result success with zero`() {
        // GIVEN
        val type = CodeType.Generic("Result", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        val call = result
        assertEquals("Result.success", call.name)
        assertEquals("0", (call.arguments[0] as CodeExpression.NumberLiteral).value)
    }

    // ===========================================
    // Collection Strategy
    // ===========================================

    @Test
    fun `GIVEN List type WHEN resolving THEN returns emptyList`() {
        // GIVEN
        val type = CodeType.Generic("List", listOf(CodeType.Simple("String")))

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        assertEquals("emptyList", result.name)
    }

    @Test
    fun `GIVEN Set type WHEN resolving THEN returns emptySet`() {
        // GIVEN
        val type = CodeType.Generic("Set", listOf(CodeType.Simple("Int")))

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertEquals("emptySet", (result as CodeExpression.FunctionCall).name)
    }

    @Test
    fun `GIVEN Map type WHEN resolving THEN returns emptyMap`() {
        // GIVEN
        val type =
            CodeType.Generic(
                "Map",
                listOf(CodeType.Simple("String"), CodeType.Simple("Int")),
            )

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertEquals("emptyMap", (result as CodeExpression.FunctionCall).name)
    }

    // ===========================================
    // Complex Compositions
    // ===========================================

    @Test
    fun `GIVEN StateFlow of List of String WHEN resolving THEN composes correctly`() {
        // GIVEN
        val type =
            CodeType.Generic(
                "StateFlow",
                listOf(
                    CodeType.Generic("List", listOf(CodeType.Simple("String"))),
                ),
            )

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        val call = result
        assertEquals("MutableStateFlow", call.name)
        // Inner should be emptyList()
        val innerCall = call.arguments[0] as CodeExpression.FunctionCall
        assertEquals("emptyList", innerCall.name)
    }

    @Test
    fun `GIVEN Result of StateFlow of Int WHEN resolving THEN composes correctly`() {
        // GIVEN
        val type =
            CodeType.Generic(
                "Result",
                listOf(
                    CodeType.Generic("StateFlow", listOf(CodeType.Simple("Int"))),
                ),
            )

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.FunctionCall)
        val call = result
        assertEquals("Result.success", call.name)
        // Inner should be MutableStateFlow(0)
        val innerCall = call.arguments[0] as CodeExpression.FunctionCall
        assertEquals("MutableStateFlow", innerCall.name)
    }

    // ===========================================
    // Fallback for Unsupported Types
    // ===========================================

    @Test
    fun `GIVEN custom unknown type WHEN resolving THEN returns TODO expression`() {
        // GIVEN
        val type = CodeType.Simple("CustomUnknownType")

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        val code = result.code
        assertTrue(code.contains("TODO"))
        assertTrue(code.contains("CustomUnknownType"))
    }

    @Test
    fun `GIVEN generic custom type WHEN resolving THEN returns TODO expression`() {
        // GIVEN
        val type = CodeType.Generic("CustomGeneric", listOf(CodeType.Simple("String")))

        // WHEN
        val result = resolver.resolve(type)

        // THEN
        assertTrue(result is CodeExpression.Raw)
        val code = result.code
        assertTrue(code.contains("TODO"))
    }
}
