// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.isVararg

/**
 * Shared utilities for analyzing IR nodes (properties, functions, types).
 *
 * This helper provides common analysis functions used by both InterfaceAnalyzer and ClassAnalyzer,
 * promoting code reuse and maintaining consistency in IR analysis across different declaration types.
 */
internal object IrAnalysisHelper {
    /**
     * Analyzes a property declaration to extract type and nullability information.
     *
     * @param property The property IR node to analyze
     * @return Property analysis with type information
     */
    fun analyzeProperty(property: IrProperty): PropertyAnalysis {
        val propertyType =
            property.getter?.returnType ?: property.backingField?.type
                ?: error("Property ${property.name} has no determinable type")

        return PropertyAnalysis(
            name = property.name.asString(),
            type = propertyType,
            isMutable = property.isVar,
            isNullable = propertyType.isMarkedNullable(),
            irProperty = property,
        )
    }

    /**
     * Analyzes a function declaration to extract signature, parameters, and type information.
     *
     * @param function The function IR node to analyze
     * @return Function analysis with complete signature information
     */
    fun analyzeFunction(function: IrSimpleFunction): FunctionAnalysis {
        // Filter for regular parameters only (excludes receiver parameters, etc.)
        val parameters =
            function.parameters
                .filter { it.kind == IrParameterKind.Regular }
                .map { param ->
                    ParameterAnalysis(
                        name = param.name.asString(),
                        type = param.type,
                        hasDefaultValue = param.defaultValue != null,
                        defaultValueCode = null, // Phase 3C.4: TODO - render IR default value if needed for legacy path
                        isVararg = param.isVararg,
                    )
                }

        // Extract type parameter bounds (e.g., R : TValue)
        val typeParameterBounds =
            function.typeParameters.associate { typeParam ->
                val paramName = typeParam.name.asString()

                val bounds =
                    if (typeParam.superTypes.isNotEmpty()) {
                        // Check all supertypes and find the first one that's not "Any"
                        val explicitBound =
                            typeParam.superTypes
                                .map { convertIrTypeToString(it) }
                                .firstOrNull { it != "Any" }

                        explicitBound ?: "Any" // Use explicit bound or default to Any
                    } else {
                        "Any" // Default bound when no explicit bounds
                    }
                paramName to bounds
            }

        return FunctionAnalysis(
            name = function.name.asString(),
            parameters = parameters,
            returnType = function.returnType,
            isSuspend = function.isSuspend,
            isInline = function.isInline,
            typeParameters =
                function.typeParameters.map { typeParam ->
                    formatTypeParameterWithConstraints(typeParam)
                },
            typeParameterBounds = typeParameterBounds,
            irFunction = function,
        )
    }

    /**
     * Determines if a function is a special compiler-generated function that should be skipped.
     *
     * @param function The function to check
     * @return true if this is a special function to skip, false otherwise
     */
    fun isSpecialFunction(function: IrSimpleFunction): Boolean {
        val name = function.name.asString()
        return name in setOf("equals", "hashCode", "toString") ||
            name.startsWith("component") ||
            name == "copy"
    }

    /**
     * Converts an IR type to a simple string representation for bound analysis.
     * This is a simplified version that focuses on extracting bound names.
     */
    fun convertIrTypeToString(irType: IrType): String =
        when {
            irType.isAny() -> "Any"
            // Handle type parameters (like TValue, TKey)
            irType is IrSimpleType && irType.classifier.owner is IrTypeParameter -> {
                val typeParam = irType.classifier.owner as IrTypeParameter
                typeParam.name.asString()
            }
            // Handle regular class types
            else -> {
                val irClass = irType.getClass()
                irClass?.name?.asString() ?: "Any"
            }
        }

    /**
     * Formats a type parameter with its constraints (bounds).
     * Examples:
     * - T -> "T"
     * - T with Comparable<T> bound -> "T : Comparable<T>"
     * - T with multiple bounds -> "T : Comparable<T>, Serializable"
     */
    fun formatTypeParameterWithConstraints(typeParam: IrTypeParameter): String {
        val name = typeParam.name.asString()

        // Get all upper bounds from IR
        val allBounds = typeParam.superTypes

        // Format each bound
        val formattedBounds =
            allBounds
                .map { bound -> formatIrTypeWithTypeArguments(bound) }
                .filter { it != "Any" && it != "Any?" } // Filter out implicit Any bounds

        if (formattedBounds.isEmpty()) {
            return name
        }

        return "$name : ${formattedBounds.joinToString(", ")}"
    }

    /**
     * Formats an IrType with its type arguments (e.g., Comparable<T>).
     * This is used for formatting type parameter constraints.
     */
    fun formatIrTypeWithTypeArguments(irType: IrType): String =
        when {
            irType !is IrSimpleType -> convertIrTypeToString(irType)
            irType.arguments.isEmpty() -> {
                when (val owner = irType.classifier.owner) {
                    is IrClass -> owner.name.asString()
                    is IrTypeParameter -> owner.name.asString()
                    else -> "Any"
                }
            }
            else -> {
                val baseName =
                    when (val owner = irType.classifier.owner) {
                        is IrClass -> owner.name.asString()
                        is IrTypeParameter -> owner.name.asString()
                        else -> "Any"
                    }
                val typeArgs =
                    irType.arguments.joinToString(", ") { arg ->
                        when (arg) {
                            is IrTypeProjection -> formatIrTypeWithTypeArguments(arg.type)
                            is IrStarProjection -> "*"
                        }
                    }
                "$baseName<$typeArgs>"
            }
        }
}

/**
 * Analysis of a property within an interface or class.
 */
data class PropertyAnalysis(
    val name: String,
    val type: IrType,
    val isMutable: Boolean,
    val isNullable: Boolean,
    val irProperty: IrProperty,
)

/**
 * Analysis of a function within an interface or class.
 */
data class FunctionAnalysis(
    val name: String,
    val parameters: List<ParameterAnalysis>,
    val returnType: IrType,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<String>, // Method-level type parameters like <T>, <T, R>
    val typeParameterBounds: Map<String, String>, // Type parameter bounds like R : TValue
    val irFunction: IrSimpleFunction,
)

/**
 * Analysis of a function parameter.
 */
data class ParameterAnalysis(
    val name: String,
    val type: IrType,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?, // Phase 3C.4: Rendered default value expression
    val isVararg: Boolean,
)
