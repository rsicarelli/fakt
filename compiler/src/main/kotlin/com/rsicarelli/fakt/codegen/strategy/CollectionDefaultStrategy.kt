// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType

/**
 * Strategy for generating default values for Kotlin collection types.
 *
 * Supports standard collections with empty initializers:
 * - Collection<T> → emptyList() (Collection is abstract, List is concrete impl)
 * - List<T> → emptyList()
 * - Set<T> → emptySet()
 * - Map<K,V> → emptyMap()
 * - Array<T> → emptyArray() (or emptyArray<Any>() + cast for class-level generics)
 * - MutableList<T> → mutableListOf()
 * - MutableSet<T> → mutableSetOf()
 * - MutableMap<K,V> → mutableMapOf()
 *
 * For Array<T> where T is a class-level type parameter, uses emptyArray<Any>() with cast
 * since emptyArray<T>() requires reified T which isn't available at runtime.
 *
 * Example:
 * ```kotlin
 * val strategy = CollectionDefaultStrategy()
 * val type = CodeType.Generic("List", listOf(CodeType.Simple("String")))
 *
 * if (strategy.supports(type)) {
 *     val default = strategy.defaultValue(type)  // FunctionCall("emptyList")
 * }
 * ```
 */
public class CollectionDefaultStrategy(
    private val classLevelTypeParams: Set<String> = emptySet()
) : DefaultValueStrategy {

    override fun supports(type: CodeType): Boolean {
        return type is CodeType.Generic && type.name in COLLECTION_TYPES
    }

    override fun defaultValue(type: CodeType): CodeExpression {
        require(supports(type)) {
            "CollectionDefaultStrategy does not support type: $type"
        }

        val typeName = (type as CodeType.Generic).name

        // Special handling for Array with class-level generics
        if (typeName == "Array" && type.arguments.isNotEmpty()) {
            val elementType = type.arguments[0]
            val elementTypeName = when (elementType) {
                is CodeType.Simple -> elementType.name
                is CodeType.Generic -> elementType.name
                is CodeType.Nullable -> when (val inner = elementType.inner) {
                    is CodeType.Simple -> inner.name
                    is CodeType.Generic -> inner.name
                    else -> null
                }
                else -> null
            }

            if (elementTypeName != null && classLevelTypeParams.contains(elementTypeName)) {
                // Use emptyArray<Any>() for class-level generics (not reified)
                return CodeExpression.Raw("@Suppress(\"UNCHECKED_CAST\") emptyArray<Any>() as Array<$elementTypeName>")
            }
        }

        val functionName = when (typeName) {
            "Collection" -> "emptyList"  // Collection is abstract, use List as concrete impl
            "List" -> "emptyList"
            "Set" -> "emptySet"
            "Map" -> "emptyMap"
            "Array" -> "emptyArray"
            "MutableList" -> "mutableListOf"
            "MutableSet" -> "mutableSetOf"
            "MutableMap" -> "mutableMapOf"
            else -> error("Unsupported collection type: $typeName")
        }

        return CodeExpression.FunctionCall(functionName)
    }

    private companion object {
        private val COLLECTION_TYPES = setOf(
            "Collection",  // Abstract collection interface
            "List",
            "Set",
            "Map",
            "Array",
            "MutableList",
            "MutableSet",
            "MutableMap"
        )
    }
}
