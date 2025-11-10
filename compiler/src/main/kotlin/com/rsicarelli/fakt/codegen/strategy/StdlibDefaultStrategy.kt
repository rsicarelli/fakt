// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType

/**
 * Strategy for generating default values for Kotlin stdlib types.
 *
 * Supports common stdlib types with intelligent defaults:
 * - Unit → Unit
 * - Flow<T> → emptyFlow()
 * - StateFlow<T> → MutableStateFlow(default(T))
 * - MutableStateFlow<T> → MutableStateFlow(default(T))
 * - Result<T> → Result.success(default(T))
 *
 * For types with generic parameters (StateFlow, Result), this strategy
 * composes with other strategies to resolve nested type defaults.
 *
 * Example:
 * ```kotlin
 * val strategy = StdlibDefaultStrategy()
 * val type = CodeType.Generic("StateFlow", listOf(CodeType.Simple("Int")))
 *
 * if (strategy.supports(type)) {
 *     val default = strategy.defaultValue(type)
 *     // FunctionCall("MutableStateFlow", [NumberLiteral("0")])
 * }
 * ```
 */
public class StdlibDefaultStrategy : DefaultValueStrategy {

    private val primitiveStrategy = PrimitiveDefaultStrategy()
    private val collectionStrategy = CollectionDefaultStrategy()

    override fun supports(type: CodeType): Boolean {
        return when (type) {
            is CodeType.Simple -> type.name == "Unit"
            is CodeType.Generic -> type.name in STDLIB_TYPES
            else -> false
        }
    }

    override fun defaultValue(type: CodeType): CodeExpression {
        require(supports(type)) {
            "StdlibDefaultStrategy does not support type: $type"
        }

        return when (type) {
            is CodeType.Simple -> {
                when (type.name) {
                    "Unit" -> CodeExpression.Raw("Unit")
                    else -> error("Unsupported simple stdlib type: ${type.name}")
                }
            }

            is CodeType.Generic -> {
                when (type.name) {
                    "Flow" -> CodeExpression.FunctionCall("emptyFlow")

                    "StateFlow", "MutableStateFlow" -> {
                        val innerType = type.arguments.first()
                        val innerDefault = resolveNestedDefault(innerType)
                        CodeExpression.FunctionCall("MutableStateFlow", listOf(innerDefault))
                    }

                    "Result" -> {
                        val innerType = type.arguments.first()
                        val innerDefault = resolveNestedDefault(innerType)
                        CodeExpression.FunctionCall("Result.success", listOf(innerDefault))
                    }

                    else -> error("Unsupported generic stdlib type: ${type.name}")
                }
            }

            else -> error("Unsupported type category: $type")
        }
    }

    /**
     * Resolves default value for nested type parameters.
     *
     * Uses primitive, stdlib, and collection strategies for common types.
     * Supports recursive resolution for nested stdlib types (e.g., Result<StateFlow<Int>>).
     * Falls back to error() for unsupported types (design choice: explicit config over auto-mocking).
     */
    private fun resolveNestedDefault(type: CodeType): CodeExpression {
        return when {
            primitiveStrategy.supports(type) -> primitiveStrategy.defaultValue(type)
            supports(type) -> defaultValue(type)  // Recursive: handles nested stdlib types
            collectionStrategy.supports(type) -> collectionStrategy.defaultValue(type)
            else -> CodeExpression.Raw(
                "error(\"Type '$type' requires explicit configuration. \" + " +
                "\"Fakt prioritizes type-safety over auto-mocking. \" + " +
                "\"Configure behavior via fake factory DSL.\") as Nothing"
            )
        }
    }

    private companion object {
        private val STDLIB_TYPES = setOf(
            "Flow",
            "StateFlow",
            "MutableStateFlow",
            "Result"
        )
    }
}
