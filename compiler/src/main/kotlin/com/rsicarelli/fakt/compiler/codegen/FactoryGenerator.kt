// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

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

        val typeParameters =
            if (hasGenerics) {
                "<${analysis.typeParameters.joinToString(", ") { "reified $it" }}>"
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

            appendLine(
                "$functionSignature(configure: $configWithGenerics.() -> Unit = {}): $interfaceWithGenerics {",
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
}
