// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isMarkedNullable

/**
 * Core pattern analyzer for Fakt compile-time type safety.
 *
 * Analyzes interfaces to determine the optimal code generation strategy:
 * - Class-level generics → Generate truly generic fake classes
 * - Method-level generics → Generate specialized handlers
 * - Mixed generics → Generate hybrid approach
 * - No generics → Use existing simple generation
 */
class GenericPatternAnalyzer {
    /**
     * Analyze an interface to determine its generic pattern and optimal generation strategy.
     */
    fun analyzeInterface(irClass: IrClass): GenericPattern {
        val classTypeParams = irClass.typeParameters
        val methodTypeParams = extractMethodTypeParameters(irClass)

        return when {
            // Simple case: no generics at all
            classTypeParams.isEmpty() && methodTypeParams.isEmpty() -> {
                GenericPattern.NoGenerics
            }

            // Class-level generics only - perfect for generic class generation
            classTypeParams.isNotEmpty() && methodTypeParams.isEmpty() -> {
                GenericPattern.ClassLevelGenerics(
                    typeParameters = classTypeParams,
                    constraints = extractTypeConstraints(classTypeParams),
                )
            }

            // Method-level generics only - use specialized handlers
            classTypeParams.isEmpty() && methodTypeParams.isNotEmpty() -> {
                GenericPattern.MethodLevelGenerics(
                    genericMethods = methodTypeParams,
                )
            }

            // Mixed: both class and method level generics
            else -> {
                GenericPattern.MixedGenerics(
                    classTypeParameters = classTypeParams,
                    classConstraints = extractTypeConstraints(classTypeParams),
                    genericMethods = methodTypeParams,
                )
            }
        }
    }

    /**
     * Extract method-level type parameters from all functions in the interface.
     */
    private fun extractMethodTypeParameters(irClass: IrClass): List<GenericMethod> =
        irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.typeParameters.isNotEmpty() }
            .map { function ->
                GenericMethod(
                    name = function.name.asString(),
                    typeParameters = function.typeParameters,
                    constraints = extractTypeConstraints(function.typeParameters),
                    parameters =
                        function.parameters
                            .filter { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
                            .map { param ->
                                MethodParameter(
                                    name = param.name.asString(),
                                    type = param.type,
                                    isVararg = param.varargElementType != null,
                                )
                            },
                    returnType = function.returnType,
                    isSuspend = function.isSuspend,
                )
            }

    /**
     * Extract type constraints (where clauses) from type parameters.
     */
    private fun extractTypeConstraints(typeParameters: List<IrTypeParameter>): List<TypeConstraint> =
        typeParameters.flatMap { typeParam ->
            typeParam.superTypes.map { superType ->
                TypeConstraint(
                    typeParameter = typeParam.name.asString(),
                    constraint = irTypeToString(superType),
                    constraintType = superType,
                )
            }
        }

    /**
     * Convert IrType to string representation for analysis.
     */
    private fun irTypeToString(irType: IrType): String =
        when {
            irType is IrSimpleType && irType.classifier is IrTypeParameterSymbol -> {
                val typeParam = irType.classifier as IrTypeParameterSymbol
                val paramName = typeParam.owner.name.asString()
                if (irType.isMarkedNullable()) "$paramName?" else paramName
            }
            irType is IrSimpleType -> {
                // Build full qualified name with type arguments
                val classifier = irType.classifier
                val baseName =
                    classifier
                        .toString()
                        .substringAfterLast('/')
                        .substringAfterLast('.')

                val typeArguments =
                    if (irType.arguments.isNotEmpty()) {
                        val args =
                            irType.arguments.mapNotNull { arg ->
                                when (arg) {
                                    is IrType -> irTypeToString(arg)
                                    else -> null
                                }
                            }
                        if (args.isNotEmpty()) "<${args.joinToString(", ")}>" else ""
                    } else {
                        ""
                    }

                val fullType = baseName + typeArguments
                if (irType.isMarkedNullable()) "$fullType?" else fullType
            }
            else -> {
                // Fallback to toString for other types
                val typeString = irType.toString()
                // Clean up common IR type representations
                typeString
                    .substringAfterLast('/')
                    .substringAfterLast('.')
                    .replace("IrClass", "")
            }
        }

    companion object {
        /**
         * Validate the analyzed pattern for consistency and completeness.
         *
         * @param pattern The generic pattern to validate
         * @param irClass The IR class being validated
         * @return List of validation warnings (empty if valid)
         */
        fun validatePattern(
            pattern: GenericPattern,
            irClass: IrClass,
        ): List<String> {
            val warnings = mutableListOf<String>()

            when (pattern) {
                is GenericPattern.ClassLevelGenerics -> {
                    if (pattern.typeParameters.isEmpty()) {
                        warnings.add("ClassLevelGenerics pattern has no type parameters")
                    }

                    // Validate constraints are properly extracted
                    pattern.constraints.forEach { constraint ->
                        if (constraint.constraint.isBlank()) {
                            warnings.add("Empty constraint found for type parameter ${constraint.typeParameter}")
                        }
                    }
                }

                is GenericPattern.MethodLevelGenerics -> {
                    if (pattern.genericMethods.isEmpty()) {
                        warnings.add("MethodLevelGenerics pattern has no generic methods")
                    }
                }

                is GenericPattern.MixedGenerics -> {
                    if (pattern.classTypeParameters.isEmpty() && pattern.genericMethods.isEmpty()) {
                        warnings.add("MixedGenerics pattern has neither class nor method generics")
                    }
                }

                GenericPattern.NoGenerics -> {
                    // Verify there really are no generics
                    val hasClassGenerics = irClass.typeParameters.isNotEmpty()
                    val hasMethodGenerics =
                        irClass.declarations
                            .filterIsInstance<IrSimpleFunction>()
                            .any { it.typeParameters.isNotEmpty() }

                    if (hasClassGenerics || hasMethodGenerics) {
                        warnings.add("Interface has generics but classified as NoGenerics")
                    }
                }
            }

            return warnings
        }

        /**
         * Get a summary of the analysis results for debugging.
         *
         * @param pattern The generic pattern to summarize
         * @return Human-readable summary string
         */
        fun getAnalysisSummary(pattern: GenericPattern): String =
            when (pattern) {
                GenericPattern.NoGenerics ->
                    "No generic parameters detected - using simple generation"

                is GenericPattern.ClassLevelGenerics ->
                    "Class-level generics: ${pattern.typeParameters.size} type parameters, " +
                        "${pattern.constraints.size} constraints"

                is GenericPattern.MethodLevelGenerics ->
                    "Method-level generics: ${pattern.genericMethods.size} generic methods"

                is GenericPattern.MixedGenerics ->
                    "Mixed generics: ${pattern.classTypeParameters.size} class type parameters, " +
                        "${pattern.genericMethods.size} generic methods"
            }
    }
}

/**
 * Sealed class representing different generic patterns found in interfaces.
 */
sealed class GenericPattern {
    /**
     * Interface has no generic parameters at all.
     * Use existing simple generation approach.
     */
    object NoGenerics : GenericPattern()

    /**
     * Interface has generic parameters at class level only.
     * Generate truly generic fake class with full type safety.
     */
    data class ClassLevelGenerics(
        val typeParameters: List<IrTypeParameter>,
        val constraints: List<TypeConstraint>,
    ) : GenericPattern()

    /**
     * Interface has generic parameters at method level only.
     * Generate specialized handlers using IR type information.
     */
    data class MethodLevelGenerics(
        val genericMethods: List<GenericMethod>,
    ) : GenericPattern()

    /**
     * Interface has both class-level and method-level generics.
     * Generate hybrid approach combining both strategies.
     */
    data class MixedGenerics(
        val classTypeParameters: List<IrTypeParameter>,
        val classConstraints: List<TypeConstraint>,
        val genericMethods: List<GenericMethod>,
    ) : GenericPattern()
}

/**
 * Represents a generic method with its type parameters and constraints.
 */
data class GenericMethod(
    val name: String,
    val typeParameters: List<IrTypeParameter>,
    val constraints: List<TypeConstraint>,
    val parameters: List<MethodParameter>,
    val returnType: IrType,
    val isSuspend: Boolean,
)

/**
 * Represents a method parameter.
 */
data class MethodParameter(
    val name: String,
    val type: IrType,
    val isVararg: Boolean,
)

/**
 * Represents a type constraint (where clause).
 */
data class TypeConstraint(
    val typeParameter: String,
    val constraint: String,
    val constraintType: IrType,
)
