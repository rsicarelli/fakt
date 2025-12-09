// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isByte
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isShort
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNotNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Renders IR types to Kotlin string representations.
 *
 * Focuses on converting IrType to readable Kotlin code strings,
 * handling primitives, nullability, and complex types.
 *
 * Uses memoization to cache type string conversions for performance.
 */
internal class TypeRenderer(
    private val genericTypeHandler: GenericTypeHandler,
    private val functionTypeHandler: FunctionTypeHandler,
) {
    /**
     * Thread-safe cache for type string conversions.
     * Key: Pair(IrType, preserveTypeParameters flag)
     * Value: Rendered string representation
     */
    private val typeStringCache = ConcurrentHashMap<Pair<IrType, Boolean>, String>()

    /**
     * Renders IR type to Kotlin string representation with optional type parameter preservation.
     *
     * Uses memoization to avoid repeated expensive type conversions.
     *
     * @param irType The IR type to render
     * @param preserveTypeParameters Whether to preserve generic type parameters
     * @return String representation of the type
     */
    fun render(
        irType: IrType,
        preserveTypeParameters: Boolean,
    ): String =
        typeStringCache.getOrPut(irType to preserveTypeParameters) {
            when {
                // Handle nullable types
                irType.isMarkedNullable() -> {
                    val baseType = render(irType.makeNotNull(), preserveTypeParameters)
                    val nonNullType = irType.makeNotNull()

                    // Function types need parentheses when nullable: ((Int, Int) -> Unit)?
                    if (functionTypeHandler.isFunction(nonNullType) ||
                        functionTypeHandler.isSuspendFunction(nonNullType)
                    ) {
                        "($baseType)?"
                    } else {
                        "$baseType?"
                    }
                }

                // Handle primitive types
                else ->
                    irType.asPrimitiveName()
                        ?: handleComplexType(irType, preserveTypeParameters)
            }
        }

    /**
     * Check if a type is primitive and doesn't need imports.
     */
    fun isPrimitive(irType: IrType): Boolean =
        irType.isString() || irType.isInt() || irType.isBoolean() ||
            irType.isUnit() || irType.isLong() || irType.isFloat() ||
            irType.isDouble() || irType.isChar() || irType.isByte() ||
            irType.isShort()

    /**
     * Returns primitive type name or null if not primitive.
     */
    private fun IrType.asPrimitiveName(): String? =
        when {
            isString() -> "String"
            isInt() -> "Int"
            isBoolean() -> "Boolean"
            isUnit() -> "Unit"
            isLong() -> "Long"
            isFloat() -> "Float"
            isDouble() -> "Double"
            isChar() -> "Char"
            isByte() -> "Byte"
            isShort() -> "Short"
            isNothing() -> "Nothing"
            isAny() -> "Any"
            else -> null
        }

    /**
     * Handles complex (non-primitive) type conversion.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun handleComplexType(
        irType: IrType,
        preserveTypeParameters: Boolean,
    ): String =
        when {
            // Handle function types
            functionTypeHandler.isFunction(irType) ->
                functionTypeHandler.renderFunctionType(
                    irType,
                    preserveTypeParameters,
                    this::render,
                )

            // Handle suspending function types
            functionTypeHandler.isSuspendFunction(irType) -> {
                val baseFunctionType =
                    functionTypeHandler.renderFunctionType(
                        irType,
                        preserveTypeParameters,
                        this::render,
                    )
                "suspend $baseFunctionType"
            }

            // Handle type parameters (T, K, V, etc.)
            irType is IrSimpleType && irType.classifier.owner is IrTypeParameter -> {
                val typeParam = irType.classifier.owner as IrTypeParameter
                if (preserveTypeParameters) {
                    typeParam.name.asString()
                } else {
                    // For NoGenerics pattern, use Any for type erasure
                    "Any"
                }
            }

            // Handle generic types
            irType is IrSimpleType && irType.arguments.isNotEmpty() -> {
                genericTypeHandler.renderGenericType(irType, preserveTypeParameters, this::render)
            }

            // Handle regular class types
            else -> {
                val irClass = irType.getClass()
                if (irClass != null) {
                    getSimpleClassName(irClass)
                } else {
                    // Fallback for unresolved types
                    irType.toString().substringAfterLast('.')
                }
            }
        }

    /**
     * Gets simple class name from IR class, avoiding package qualification.
     */
    private fun getSimpleClassName(irClass: IrClass): String = irClass.name.asString()
}
