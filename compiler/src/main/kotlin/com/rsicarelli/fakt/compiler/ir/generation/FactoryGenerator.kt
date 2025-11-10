// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis

/**
 * Generates factory functions for fake implementations.
 * Creates type-safe factory functions that provide convenient instantiation with configuration.
 */
internal class FactoryGenerator {
    /**
     * Generates a factory function for the fake implementation.
     *
     * @param analysis The analyzed interface metadata
     * @param fakeClassName The name of the fake implementation class
     * @return The generated factory function code
     */
    fun generateFactoryFunction(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
    ): String {
        val interfaceName = analysis.interfaceName
        val factoryFunctionName = "fake$interfaceName"
        val configClassName = "Fake${interfaceName}Config"

        // Generate reified generic factory function
        val hasGenerics = analysis.typeParameters.isNotEmpty()

        // Extract type parameter names (without constraints) for use as type arguments
        val typeParameterNames =
            if (hasGenerics) {
                analysis.typeParameters.map { it.substringBefore(" :").trim() }
            } else {
                emptyList()
            }

        // Handle where clause for multiple constraints
        val (typeParamsForHeader, whereClause) = formatTypeParametersWithWhereClause(analysis.typeParameters)

        val typeParameters =
            if (hasGenerics) {
                "<${typeParamsForHeader.joinToString(", ") { "reified $it" }}>"
            } else {
                ""
            }

        val configWithGenerics =
            if (hasGenerics) {
                "$configClassName<${typeParameterNames.joinToString(", ")}>"
            } else {
                configClassName
            }

        return buildString {
            val functionSignature = buildFunctionSignature(hasGenerics, typeParameters, factoryFunctionName)
            val whereClausePart = if (whereClause.isNotEmpty()) " where $whereClause" else ""
            val returnType = buildReturnType(fakeClassName, typeParameterNames)

            appendLine(
                "$functionSignature(" +
                    "configure: $configWithGenerics.() -> Unit = {}" +
                    "): $returnType$whereClausePart {",
            )

            // Use simple type parameter names for constructor (not reified, no constraints)
            val constructorTypeParams =
                if (hasGenerics) {
                    "<${typeParameterNames.joinToString(", ")}>"
                } else {
                    ""
                }

            appendLine(
                "    return $fakeClassName$constructorTypeParams().apply { " +
                    "$configWithGenerics(this).configure() }",
            )
            appendLine("}")
        }
    }

    /**
     * Generates a factory function for the fake class implementation.
     *
     * @param analysis The analyzed class metadata
     * @param fakeClassName The name of the fake implementation class
     * @return The generated factory function code
     */
    fun generateFactoryFunction(
        analysis: ClassAnalysis,
        fakeClassName: String,
    ): String {
        val className = analysis.className
        val factoryFunctionName = "fake$className"
        val configClassName = "Fake${className}Config"

        // Format type parameters with where clause for multiple constraints
        val (typeParamsForHeader, whereClause) = formatTypeParametersWithWhereClause(analysis.typeParameters)

        val typeParameters =
            if (typeParamsForHeader.isNotEmpty()) {
                "<${typeParamsForHeader.joinToString(", ")}>"
            } else {
                ""
            }

        // Extract type parameter names (without constraints) for return type
        val typeParameterNames =
            if (analysis.typeParameters.isNotEmpty()) {
                analysis.typeParameters.map { it.substringBefore(" :").trim() }
            } else {
                emptyList()
            }

        // Type arguments for usage (just names, no constraints)
        val typeArguments =
            if (typeParameterNames.isNotEmpty()) {
                "<${typeParameterNames.joinToString(", ")}>"
            } else {
                ""
            }

        return buildString {
            val hasGenerics = typeParameters.isNotEmpty()
            val functionSignature = buildFunctionSignature(hasGenerics, typeParameters, factoryFunctionName)
            val returnType = buildReturnType(fakeClassName, typeParameterNames)

            // Generate factory function signature with where clause if needed
            if (whereClause.isNotEmpty()) {
                appendLine(
                    "$functionSignature(" +
                        "configure: $configClassName$typeArguments.() -> Unit = {}" +
                        "): $returnType where $whereClause {",
                )
            } else {
                appendLine(
                    "$functionSignature(" +
                        "configure: $configClassName$typeArguments.() -> Unit = {}" +
                        "): $returnType {",
                )
            }

            appendLine(
                "    return $fakeClassName$typeArguments().apply { " +
                    "$configClassName$typeArguments(this).configure() }",
            )
            appendLine("}")
        }
    }

    /**
     * Builds the return type for the factory function.
     * Returns the fake implementation class name with generic parameters if applicable.
     */
    private fun buildReturnType(
        fakeClassName: String,
        typeParameterNames: List<String>,
    ): String =
        if (typeParameterNames.isNotEmpty()) {
            "$fakeClassName<${typeParameterNames.joinToString(", ")}>"
        } else {
            fakeClassName
        }

    /**
     * Builds the function signature for the factory function.
     */
    private fun buildFunctionSignature(
        hasGenerics: Boolean,
        typeParameters: String,
        factoryFunctionName: String,
    ): String =
        if (hasGenerics) {
            "inline fun $typeParameters $factoryFunctionName"
        } else {
            "fun $factoryFunctionName"
        }

    /**
     * Formats type parameters for factory function headers, handling where clauses for multiple constraints.
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
