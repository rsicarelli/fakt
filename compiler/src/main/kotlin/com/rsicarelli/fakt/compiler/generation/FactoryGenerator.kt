// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.generation

import com.rsicarelli.fakt.compiler.analysis.InterfaceAnalysis

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
     * @param packageName The package name for type references
     * @return The generated factory function code
     */
    fun generateFactoryFunction(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
        packageName: String
    ): String {
        val interfaceName = analysis.interfaceName
        val factoryFunctionName = "fake${interfaceName}"
        val configClassName = "Fake${interfaceName}Config"

        // Handle interface-level generics for NoGenerics pattern
        val interfaceWithGenerics = if (analysis.typeParameters.isNotEmpty()) {
            val genericParams = analysis.typeParameters.joinToString(", ") { "Any" }
            "$interfaceName<$genericParams>"
        } else {
            interfaceName
        }

        return buildString {
            appendLine("fun $factoryFunctionName(configure: $configClassName.() -> Unit = {}): $interfaceWithGenerics {")
            appendLine("    return $fakeClassName().apply { $configClassName(this).configure() }")
            appendLine("}")
        }
    }
}
