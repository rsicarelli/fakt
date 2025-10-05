// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.ParameterAnalysis
import com.rsicarelli.fakt.compiler.types.TypeResolver
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Generates configuration DSL classes for fake implementations.
 * Creates type-safe DSL classes that provide convenient configuration of fake behavior.
 *
 * @since 1.0.0
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ConfigurationDslGenerator(
    private val typeResolver: TypeResolver,
) {
    companion object {
        /**
         * Length of "Array<" prefix when extracting generic type from Array<T>.
         */
        private const val ARRAY_PREFIX_LENGTH = 6
    }

    /**
     * Generates a configuration DSL class for the fake implementation.
     *
     * @param analysis The analyzed interface metadata
     * @param fakeClassName The name of the fake implementation class
     * @return The generated configuration DSL class code
     */
    fun generateConfigurationDsl(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
    ): String {
        val interfaceName = analysis.interfaceName
        val configClassName = "Fake${interfaceName}Config"

        // Handle where clause for multiple constraints
        val (typeParamsForHeader, whereClause) = formatTypeParametersWithWhereClause(analysis.typeParameters)

        // Phase 2: Add type parameters to config class
        val typeParameters =
            if (typeParamsForHeader.isNotEmpty()) {
                "<${typeParamsForHeader.joinToString(", ")}>"
            } else {
                ""
            }

        // Extract type parameter names (without constraints) for use as type arguments
        val typeParameterNames =
            if (analysis.typeParameters.isNotEmpty()) {
                "<${analysis.typeParameters.joinToString(", ") { it.substringBefore(" :").trim() }}>"
            } else {
                ""
            }

        return buildString {
            // Generate class header with optional where clause (after constructor for classes!)
            if (whereClause.isNotEmpty()) {
                appendLine("class $configClassName$typeParameters(private val fake: $fakeClassName$typeParameterNames) where $whereClause {")
            } else {
                appendLine("class $configClassName$typeParameters(private val fake: $fakeClassName$typeParameterNames) {")
            }

            // Generate configuration methods for functions (TYPE-SAFE: Use exact types)
            for (function in analysis.functions) {
                val functionName = function.name
                val hasMethodGenerics = function.typeParameters.isNotEmpty()

                // Phase 3: Add method-level type parameters to DSL methods
                val methodTypeParams =
                    if (hasMethodGenerics) {
                        "<${function.typeParameters.joinToString(", ")}> "
                    } else {
                        ""
                    }

                val parameterTypes = buildParameterTypeString(function.parameters)

                val returnType =
                    typeResolver.irTypeToKotlinString(
                        function.returnType,
                        preserveTypeParameters = true,
                    )
                val suspendModifier = if (function.isSuspend) "suspend " else ""
                appendLine(
                    "    fun $methodTypeParams$functionName(behavior: $suspendModifier($parameterTypes) -> $returnType) " +
                        "{ fake.configure${functionName.capitalize()}(behavior) }",
                )
            }

            // Generate configuration methods for properties (TYPE-SAFE: Use exact types)
            for (property in analysis.properties) {
                val propertyName = property.name
                val propertyType =
                    typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
                appendLine(
                    "    fun $propertyName(behavior: () -> $propertyType) " +
                        "{ fake.configure${propertyName.capitalize()}(behavior) }",
                )
            }

            append("}")
        }
    }

    /**
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }

    /**
     * Builds parameter type string for configuration DSL methods.
     *
     * @param parameters List of parameters to process
     * @return Comma-separated parameter type string with varargs support
     */
    private fun buildParameterTypeString(parameters: List<ParameterAnalysis>): String =
        if (parameters.isEmpty()) {
            ""
        } else {
            parameters.joinToString(", ") { param ->
                val varargsPrefix = if (param.isVararg) "vararg " else ""
                val paramType = resolveParameterType(param)
                varargsPrefix + paramType
            }
        }

    /**
     * Resolves the Kotlin type string for a parameter, unwrapping varargs if needed.
     *
     * @param param The parameter to resolve
     * @return The Kotlin type string
     */
    private fun resolveParameterType(param: ParameterAnalysis): String =
        if (param.isVararg) {
            unwrapVarargsType(param)
        } else {
            typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
        }

    /**
     * Unwraps varargs Array<T> to element type T.
     *
     * @param param The varargs parameter
     * @return The unwrapped element type
     */
    private fun unwrapVarargsType(param: ParameterAnalysis): String {
        val arrayType = typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
        return if (arrayType.startsWith("Array<") && arrayType.endsWith(">")) {
            arrayType.substring(ARRAY_PREFIX_LENGTH, arrayType.length - 1)
        } else {
            "String" // Safe fallback for varargs
        }
    }

    /**
     * Formats type parameters for config class headers, handling where clauses for multiple constraints.
     * Same logic as ImplementationGenerator's formatTypeParametersWithWhereClause.
     */
    private fun formatTypeParametersWithWhereClause(typeParameters: List<String>): Pair<List<String>, String> {
        if (typeParameters.isEmpty()) {
            return emptyList<String>() to ""
        }

        val paramsForHeader = mutableListOf<String>()
        val whereClauses = mutableListOf<String>()

        for (typeParam in typeParameters) {
            val colonIndex = typeParam.indexOf(" :")
            if (colonIndex == -1) {
                paramsForHeader.add(typeParam)
                continue
            }

            val name = typeParam.substring(0, colonIndex).trim()
            val constraints = typeParam.substring(colonIndex + 2).trim()
            val constraintList = constraints.split(",").map { it.trim() }

            if (constraintList.size == 1) {
                paramsForHeader.add(typeParam)
            } else {
                paramsForHeader.add(name)
                constraintList.forEach { constraint ->
                    whereClauses.add("$name : $constraint")
                }
            }
        }

        return paramsForHeader to whereClauses.joinToString(", ")
    }
}
