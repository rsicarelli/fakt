// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Handles function type rendering and detection.
 *
 * Responsible for:
 * - Detecting function types (Function0, Function1, etc.)
 * - Detecting suspend function types
 * - Rendering function types to Kotlin syntax: (A, B) -> R
 */
internal class FunctionTypeHandler {
    /**
     * Checks if an IR type represents a function type.
     */
    fun isFunction(irType: IrType): Boolean {
        val irClass = irType.getClass() ?: return false
        val className = irClass.name.asString()
        val packageName = irClass.kotlinFqName.parent().asString()
        return packageName == "kotlin" && className.startsWith("Function")
    }

    /**
     * Checks if an IR type represents a suspend function type.
     */
    fun isSuspendFunction(irType: IrType): Boolean {
        val irClass = irType.getClass() ?: return false
        val className = irClass.name.asString()
        val packageName = irClass.kotlinFqName.parent().asString()
        return packageName == "kotlin.coroutines" && className.startsWith("SuspendFunction")
    }

    /**
     * Renders function type to Kotlin syntax.
     *
     * @param irType The function type to render
     * @param preserveTypeParameters Whether to preserve generic type parameters
     * @param typeRenderer Function to render nested types
     * @return String representation like "(Int, String) -> Boolean"
     */
    fun renderFunctionType(
        irType: IrType,
        preserveTypeParameters: Boolean,
        typeRenderer: (IrType, Boolean) -> String,
    ): String {
        // Extract function arity from class name (Function0, Function1, etc.)
        val irClass = irType.getClass()
        val className = irClass?.name?.asString() ?: ""

        if (className.startsWith("Function") || className.startsWith("SuspendFunction")) {
            val arityString =
                when {
                    className.startsWith("SuspendFunction") -> className.removePrefix("SuspendFunction")
                    className.startsWith("Function") -> className.removePrefix("Function")
                    else -> ""
                }
            val arity = arityString.toIntOrNull() ?: 0

            if (irType is IrSimpleType && irType.arguments.size == arity + 1) {
                val paramTypes =
                    irType.arguments.take(arity).map { arg ->
                        if (arg is IrTypeProjection) {
                            typeRenderer(arg.type, preserveTypeParameters)
                        } else {
                            "Any"
                        }
                    }
                val returnType =
                    irType.arguments.lastOrNull()?.let { arg ->
                        if (arg is IrTypeProjection) {
                            typeRenderer(arg.type, preserveTypeParameters)
                        } else {
                            "Any"
                        }
                    } ?: "Unit"

                return if (paramTypes.isEmpty()) {
                    "() -> $returnType"
                } else {
                    "(${paramTypes.joinToString(", ")}) -> $returnType"
                }
            }
        }

        // Fallback
        return "() -> Unit"
    }
}
