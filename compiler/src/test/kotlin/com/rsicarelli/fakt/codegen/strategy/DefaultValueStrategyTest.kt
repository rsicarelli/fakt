// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for DefaultValueStrategy interface.
 *
 * Verifies strategy pattern contract for default value generation.
 */
class DefaultValueStrategyTest {
    @Test
    fun `GIVEN strategy implementation WHEN checking supported type THEN returns true`() {
        // GIVEN
        val strategy =
            object : DefaultValueStrategy {
                override fun supports(type: CodeType): Boolean = type is CodeType.Simple && type.name == "String"

                override fun defaultValue(type: CodeType): CodeExpression = CodeExpression.StringLiteral("")
            }
        val stringType = CodeType.Simple("String")

        // WHEN
        val result = strategy.supports(stringType)

        // THEN
        assertTrue(result)
    }

    @Test
    fun `GIVEN strategy implementation WHEN checking unsupported type THEN returns false`() {
        // GIVEN
        val strategy =
            object : DefaultValueStrategy {
                override fun supports(type: CodeType): Boolean = type is CodeType.Simple && type.name == "String"

                override fun defaultValue(type: CodeType): CodeExpression = CodeExpression.StringLiteral("")
            }
        val intType = CodeType.Simple("Int")

        // WHEN
        val result = strategy.supports(intType)

        // THEN
        assertFalse(result)
    }

    @Test
    fun `GIVEN supported type WHEN requesting default value THEN returns expression`() {
        // GIVEN
        val strategy =
            object : DefaultValueStrategy {
                override fun supports(type: CodeType): Boolean = type is CodeType.Simple && type.name == "String"

                override fun defaultValue(type: CodeType): CodeExpression = CodeExpression.StringLiteral("")
            }
        val stringType = CodeType.Simple("String")

        // WHEN
        val result = strategy.defaultValue(stringType)

        // THEN
        assertTrue(result is CodeExpression.StringLiteral)
        assertEquals("", result.value)
    }
}
