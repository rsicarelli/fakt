// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType

/**
 * Strategy for generating default values for Kotlin primitive types.
 *
 * Supports all Kotlin primitives with sensible defaults:
 * - String → ""
 * - Int → 0
 * - Long → 0L
 * - Boolean → false
 * - Double → 0.0
 * - Float → 0.0f
 * - Short → 0
 * - Byte → 0
 * - Char → ' '
 *
 * Example:
 * ```kotlin
 * val strategy = PrimitiveDefaultStrategy()
 * val type = CodeType.Simple("String")
 *
 * if (strategy.supports(type)) {
 *     val default = strategy.defaultValue(type)  // StringLiteral("")
 * }
 * ```
 */
public class PrimitiveDefaultStrategy : DefaultValueStrategy {

    override fun supports(type: CodeType): Boolean {
        return type is CodeType.Simple && type.name in PRIMITIVE_TYPES
    }

    override fun defaultValue(type: CodeType): CodeExpression {
        require(supports(type)) {
            "PrimitiveDefaultStrategy does not support type: $type"
        }

        val typeName = (type as CodeType.Simple).name
        return when (typeName) {
            "String" -> CodeExpression.StringLiteral("")
            "Int" -> CodeExpression.NumberLiteral("0")
            "Long" -> CodeExpression.NumberLiteral("0L")
            "Boolean" -> CodeExpression.Raw("false")
            "Double" -> CodeExpression.NumberLiteral("0.0")
            "Float" -> CodeExpression.NumberLiteral("0.0f")
            "Short" -> CodeExpression.NumberLiteral("0")
            "Byte" -> CodeExpression.NumberLiteral("0")
            "Char" -> CodeExpression.Raw("' '")
            else -> error("Unsupported primitive type: $typeName")
        }
    }

    private companion object {
        private val PRIMITIVE_TYPES = setOf(
            "String",
            "Int",
            "Long",
            "Boolean",
            "Double",
            "Float",
            "Short",
            "Byte",
            "Char"
        )
    }
}
