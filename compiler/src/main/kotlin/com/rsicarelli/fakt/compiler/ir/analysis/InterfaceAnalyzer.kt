// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.isVararg

/**
 * Analyzes interface structure to extract metadata for fake generation.
 *
 * This class handles the complex task of analyzing interface IR nodes
 * to extract all necessary information for generating type-safe fakes.
 *
 * Separated from the main generator to isolate analysis logic and make
 * it easier to test and maintain.
 *
 * @since 1.0.0
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
@Suppress("DEPRECATION")
internal class InterfaceAnalyzer {
    private val patternAnalyzer = GenericPatternAnalyzer()

    /**
     * Checks if interface has generics and should be skipped.
     * Provides helpful error messages for unsupported generic interfaces.
     *
     * @param irClass The interface class to check
     * @return null if supported, error message if has generics
     */
    fun checkGenericSupport(irClass: IrClass): String? =
        checkInterfaceLevelGenerics(irClass)
            ?: checkMethodLevelGenerics(irClass)

    /**
     * Checks for interface-level type parameters (e.g., interface Repo<T>).
     *
     * @param irClass The interface class to check
     * @return Error message if generics found, null otherwise
     */
    private fun checkInterfaceLevelGenerics(irClass: IrClass): String? {
        if (irClass.typeParameters.isEmpty()) return null

        val typeParams = irClass.typeParameters.joinToString(", ") { it.name.asString() }
        return "Generic interfaces not supported: ${irClass.name.asString()}<$typeParams>. " +
            "Consider creating a non-generic interface that extends this one: " +
            "interface User${irClass.name.asString()} : ${irClass.name.asString()}<User>"
    }

    /**
     * Checks for method-level type parameters (e.g., fun <T> process()).
     *
     * @param irClass The interface class to check
     * @return Error message if generics found, null otherwise
     */
    private fun checkMethodLevelGenerics(irClass: IrClass): String? {
        val functionsWithGenerics =
            irClass.declarations
                .filterIsInstance<IrSimpleFunction>()
                .filter { it.typeParameters.isNotEmpty() }

        if (functionsWithGenerics.isEmpty()) return null

        val methodName = functionsWithGenerics.first().name.asString()
        val typeParams =
            functionsWithGenerics.first().typeParameters.joinToString(", ") { it.name.asString() }
        return "Generic methods not supported: $methodName<$typeParams>. " +
            "Consider creating methods with specific types instead of generics."
    }

    fun analyzeInterfaceDynamically(sourceInterface: IrClass): InterfaceAnalysis {
        val properties = mutableListOf<PropertyAnalysis>()
        val functions = mutableListOf<FunctionAnalysis>()

        // Extract type parameters from the interface with constraints
        val typeParameters = sourceInterface.typeParameters.map { typeParam ->
            formatTypeParameterWithConstraints(typeParam)
        }

        // Analyze all declarations in the interface
        sourceInterface.declarations.forEach { declaration ->
            when (declaration) {
                is IrProperty -> {
                    // Skip if this property doesn't have a getter (shouldn't happen in interfaces)
                    if (declaration.getter != null) {
                        properties.add(analyzeProperty(declaration))
                    }
                }

                is IrSimpleFunction -> {
                    // Skip special functions (equals, hashCode, toString) and compiler-generated
                    if (!isSpecialFunction(declaration)) {
                        functions.add(analyzeFunction(declaration))
                    }
                }
            }
        }

        // Analyze generic pattern for optimal code generation
        val genericPattern = patternAnalyzer.analyzeInterface(sourceInterface)

        return InterfaceAnalysis(
            interfaceName = sourceInterface.name.asString(),
            typeParameters = typeParameters,
            properties = properties,
            functions = functions,
            sourceInterface = sourceInterface,
            genericPattern = genericPattern,
            debugInfo = StringBuilder(),
        )
    }

    /**
     * Analyzes a property declaration to extract type and nullability information.
     *
     * @param property The property IR node to analyze
     * @return Property analysis with type information
     */
    private fun analyzeProperty(property: IrProperty): PropertyAnalysis {
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
    private fun analyzeFunction(function: IrSimpleFunction): FunctionAnalysis {
        // Use modern API: function.parameters instead of deprecated valueParameters
        // Filter for regular parameters only (excludes receiver parameters, etc.)
        val parameters =
            function.parameters
                .filter { it.kind == IrParameterKind.Regular }
                .map { param ->
                    ParameterAnalysis(
                        name = param.name.asString(),
                        type = param.type,
                        hasDefaultValue = param.defaultValue != null,
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
            typeParameters = function.typeParameters.map { typeParam ->
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
    private fun isSpecialFunction(function: IrSimpleFunction): Boolean {
        val name = function.name.asString()
        return name in setOf("equals", "hashCode", "toString") ||
            name.startsWith("component") ||
            name == "copy"
    }

    /**
     * Converts an IR type to a simple string representation for bound analysis.
     * This is a simplified version that focuses on extracting bound names.
     */
    private fun convertIrTypeToString(irType: IrType): String =
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
    private fun formatTypeParameterWithConstraints(typeParam: IrTypeParameter): String {
        val name = typeParam.name.asString()

        // Get upper bounds (constraints) excluding Any
        val bounds = typeParam.superTypes.filter { !it.isAny() }

        if (bounds.isEmpty()) {
            return name
        }

        // Format each bound - convert IrType to string representation
        val formattedBounds = bounds.joinToString(", ") { bound ->
            formatIrTypeWithTypeArguments(bound)
        }

        return "$name : $formattedBounds"
    }

    /**
     * Formats an IrType with its type arguments (e.g., Comparable<T>).
     * This is used for formatting type parameter constraints.
     */
    private fun formatIrTypeWithTypeArguments(irType: IrType): String {
        if (irType !is IrSimpleType) {
            return convertIrTypeToString(irType)
        }

        val baseName = when (val owner = irType.classifier.owner) {
            is IrClass -> owner.name.asString()
            is IrTypeParameter -> owner.name.asString()
            else -> "Any"
        }

        // If no type arguments, return base name
        if (irType.arguments.isEmpty()) {
            return baseName
        }

        // Format type arguments
        val typeArgs = irType.arguments.joinToString(", ") { arg ->
            when (arg) {
                is IrTypeProjection -> formatIrTypeWithTypeArguments(arg.type)
                is IrStarProjection -> "*"
                else -> "Any"
            }
        }

        return "$baseName<$typeArgs>"
    }
}

/**
 * Complete analysis of an interface including all its members and metadata.
 */
data class InterfaceAnalysis(
    val interfaceName: String,
    val typeParameters: List<String>, // Interface-level type parameters like <T>, <K, V>
    val properties: List<PropertyAnalysis>,
    val functions: List<FunctionAnalysis>,
    val sourceInterface: IrClass,
    val genericPattern: GenericPattern, // Smart pattern analysis for optimal code generation
    val debugInfo: StringBuilder = StringBuilder(), // Debug information for troubleshooting
)

/**
 * Analysis of a property within an interface.
 */
data class PropertyAnalysis(
    val name: String,
    val type: IrType,
    val isMutable: Boolean,
    val isNullable: Boolean,
    val irProperty: IrProperty,
)

/**
 * Analysis of a function within an interface.
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
    val isVararg: Boolean,
)
