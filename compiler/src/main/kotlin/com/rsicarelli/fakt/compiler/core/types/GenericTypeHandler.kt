// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Handles generic type parameter rendering and resolution.
 *
 * Responsible for:
 * - Converting generic types with type arguments
 * - Type parameter preservation vs erasure
 * - Special handling for common generic types (List, Map, etc.)
 */
internal class GenericTypeHandler {
    /**
     * Renders generic type with proper type parameter handling.
     *
     * @param irType The generic IR type to render
     * @param preserveTypeParameters Whether to preserve generic type parameters
     * @param typeRenderer Function to render nested types
     * @return String representation like "List<T>" or "Map<K, V>"
     */
    fun renderGenericType(
        irType: IrSimpleType,
        preserveTypeParameters: Boolean,
        typeRenderer: (IrType, Boolean) -> String,
    ): String {
        val irClass = irType.getClass() ?: return "Any"
        val className = irClass.name.asString()
        val packageName = irClass.kotlinFqName.parent().asString()

        return when {
            preserveTypeParameters && irType.arguments.isNotEmpty() -> {
                val typeArgsString =
                    renderTypeArguments(
                        arguments = irType.arguments,
                        preserveTypeParameters = preserveTypeParameters,
                        typeRenderer = typeRenderer,
                    )
                "$className$typeArgsString"
            }

            // NoGenerics pattern: Use specific type erasure rules for common types
            packageName == "kotlin.collections" && className in
                listOf(
                    "List",
                    "MutableList",
                )
            -> "List<Any>"

            packageName == "kotlin.collections" && className in
                listOf(
                    "Set",
                    "MutableSet",
                )
            -> "Set<Any>"

            packageName == "kotlin.collections" && className in
                listOf(
                    "Map",
                    "MutableMap",
                )
            -> "Map<Any, Any>"

            packageName == "kotlin.collections" && className == "Collection" -> "Collection<Any>"
            packageName == "kotlin" && className == "Result" -> "Result<Any>"
            packageName == "kotlin" && className == "Array" -> "Array<Any>"
            else -> className
        }
    }

    /**
     * Converts type arguments to string representation.
     * Preserves star projections (*) for type-safe override signatures.
     *
     * @param arguments List of type arguments to convert
     * @param preserveTypeParameters Whether to preserve generic type parameters
     * @param typeRenderer Function to render nested types
     * @return String representation like "<T, R>" or "<*>" for star projections
     */
    private fun renderTypeArguments(
        arguments: List<IrTypeArgument>,
        preserveTypeParameters: Boolean,
        typeRenderer: (IrType, Boolean) -> String,
    ): String {
        if (arguments.isEmpty()) return ""

        val typeArgs =
            arguments.map { arg ->
                when (arg) {
                    is IrStarProjection -> "*" // Preserve star projections
                    is IrTypeProjection -> typeRenderer(arg.type, preserveTypeParameters)
                }
            }
        return "<${typeArgs.joinToString(", ")}>"
    }
}
