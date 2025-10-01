// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.generation

import com.rsicarelli.fakt.compiler.analysis.InterfaceAnalysis
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
    private val typeResolver: TypeResolver
) {

    /**
     * Generates a configuration DSL class for the fake implementation.
     *
     * @param analysis The analyzed interface metadata
     * @param fakeClassName The name of the fake implementation class
     * @param packageName The package name for type references
     * @return The generated configuration DSL class code
     */
    fun generateConfigurationDsl(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
        packageName: String
    ): String {
        val interfaceName = analysis.interfaceName
        val configClassName = "Fake${interfaceName}Config"

        return buildString {
            appendLine("class $configClassName(private val fake: $fakeClassName) {")

            // Generate configuration methods for functions (TYPE-SAFE: Use exact types)
            for (function in analysis.functions) {
                val functionName = function.name

                // Use EXACT parameter types for type-safe configuration
                val parameterTypes = if (function.parameters.isEmpty()) {
                    ""
                } else {
                    function.parameters.joinToString(", ") { param ->
                        val varargsPrefix = if (param.isVararg) "vararg " else ""
                        val paramType = if (param.isVararg) {
                            // For varargs, unwrap Array<T> to T
                            val arrayType = typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                            val unwrappedType = if (arrayType.startsWith("Array<") && arrayType.endsWith(">")) {
                                arrayType.substring(6, arrayType.length - 1) // Extract T from Array<T>
                            } else {
                                "String" // Safe fallback for varargs
                            }
                            unwrappedType
                        } else {
                            typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                        }
                        varargsPrefix + paramType
                    }
                }

                val returnType = typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
                val suspendModifier = if (function.isSuspend) "suspend " else ""
                appendLine("    fun $functionName(behavior: ${suspendModifier}($parameterTypes) -> $returnType) { fake.configure${functionName.capitalize()}(behavior) }")
            }

            // Generate configuration methods for properties (TYPE-SAFE: Use exact types)
            for (property in analysis.properties) {
                val propertyName = property.name
                val propertyType = typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
                appendLine("    fun $propertyName(behavior: () -> $propertyType) { fake.configure${propertyName.capitalize()}(behavior) }")
            }

            append("}")
        }
    }

    /**
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }
}
