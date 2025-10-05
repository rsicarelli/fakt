// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis

/**
 * Generates factory functions for fake implementations.
 * Creates type-safe factory functions that provide convenient instantiation with configuration.
 *
 * @since 1.0.0
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

        // Phase 2: Generate reified generic factory function
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

        val interfaceWithGenerics =
            if (hasGenerics) {
                "$interfaceName<${typeParameterNames.joinToString(", ")}>"
            } else {
                interfaceName
            }

        val configWithGenerics =
            if (hasGenerics) {
                "$configClassName<${typeParameterNames.joinToString(", ")}>"
            } else {
                configClassName
            }

        val inlineModifier = if (hasGenerics) "inline " else ""

        return buildString {
            // Format: inline fun <reified T> fakeFoo(...) or fun fakeFoo(...)
            val functionSignature =
                if (hasGenerics) {
                    "inline fun $typeParameters $factoryFunctionName"
                } else {
                    "fun $factoryFunctionName"
                }

            // Add where clause if needed (after return type for functions!)
            val whereClausePart = if (whereClause.isNotEmpty()) " where $whereClause" else ""

            appendLine(
                "$functionSignature(configure: $configWithGenerics.() -> Unit = {}): $interfaceWithGenerics$whereClausePart {",
            )

            // Phase 2: Use simple type parameter names for constructor (not reified, no constraints)
            val constructorTypeParams =
                if (hasGenerics) {
                    "<${typeParameterNames.joinToString(", ")}>"
                } else {
                    ""
                }

            appendLine("    return $fakeClassName$constructorTypeParams().apply { $configWithGenerics(this).configure() }")
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

        // Classes don't have type parameters yet (future enhancement)
        // For now, generate simple factory without generics

        return buildString {
            appendLine(
                "fun $factoryFunctionName(configure: $configClassName.() -> Unit = {}): $className {",
            )
            appendLine("    return $fakeClassName().apply { $configClassName(this).configure() }")
            appendLine("}")
        }
    }

    /**
     * Formats type parameters for factory function headers, handling where clauses for multiple constraints.
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
