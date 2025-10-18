// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.ParameterAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.PropertyAnalysis
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
                appendLine(
                    "class $configClassName$typeParameters(" +
                        "private val fake: $fakeClassName$typeParameterNames" +
                        ") where $whereClause {",
                )
            } else {
                appendLine(
                    "class $configClassName$typeParameters(" +
                        "private val fake: $fakeClassName$typeParameterNames) {",
                )
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
                    "    fun $methodTypeParams$functionName(" +
                        "behavior: $suspendModifier($parameterTypes) -> $returnType) " +
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
     * NOTE: DSL methods use function types, which CANNOT have vararg modifiers
     * Varargs are converted to Array<out T> for function type signatures
     *
     * @param parameters List of parameters to process
     * @return Comma-separated parameter type string with Array<out T> for varargs
     */
    private fun buildParameterTypeString(parameters: List<ParameterAnalysis>): String =
        if (parameters.isEmpty()) {
            ""
        } else {
            parameters.joinToString(", ") { param ->
                if (param.isVararg) {
                    // For varargs, use Array<out T> in function type (NOT vararg keyword)
                    val elementType = unwrapVarargsType(param)
                    "Array<out $elementType>"
                } else {
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                }
            }
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

    /**
     * Generates a configuration DSL class for the fake class implementation.
     *
     * @param analysis The analyzed class metadata
     * @param fakeClassName The name of the fake implementation class
     * @return The generated configuration DSL class code
     */
    fun generateConfigurationDsl(
        analysis: ClassAnalysis,
        fakeClassName: String,
    ): String {
        val className = analysis.className
        val configClassName = "Fake${className}Config"

        // Format type parameters with where clause for multiple constraints
        val (typeParamsForHeader, whereClause) = formatTypeParametersWithWhereClause(analysis.typeParameters)

        val typeParameters =
            if (typeParamsForHeader.isNotEmpty()) {
                "<${typeParamsForHeader.joinToString(", ")}>"
            } else {
                ""
            }

        // Type arguments for usage (just names, no constraints)
        val typeParameterNames =
            if (analysis.typeParameters.isNotEmpty()) {
                analysis.typeParameters.map { it.substringBefore(" :").trim() }
            } else {
                emptyList()
            }

        val typeArguments =
            if (typeParameterNames.isNotEmpty()) {
                "<${typeParameterNames.joinToString(", ")}>"
            } else {
                ""
            }

        return buildString {
            // Generate config class header with type parameters and where clause
            if (whereClause.isNotEmpty()) {
                appendLine(
                    "class $configClassName$typeParameters(" +
                        "private val fake: $fakeClassName$typeArguments" +
                        ") where $whereClause {",
                )
            } else {
                appendLine(
                    "class $configClassName$typeParameters(" +
                        "private val fake: $fakeClassName$typeArguments) {",
                )
            }

            // Generate configuration methods for abstract methods
            for (function in analysis.abstractMethods) {
                appendLine(generateConfigMethodForFunction(function))
            }

            // Generate configuration methods for open methods
            for (function in analysis.openMethods) {
                appendLine(generateConfigMethodForFunction(function))
            }

            // Generate configuration methods for abstract properties
            for (property in analysis.abstractProperties) {
                appendLine(generateConfigMethodForProperty(property))
            }

            // Generate configuration methods for open properties
            for (property in analysis.openProperties) {
                appendLine(generateConfigMethodForProperty(property))
            }

            appendLine("}")
        }
    }

    /**
     * Generates a single configuration method for a property (abstract or open).
     * Used for class DSL generation.
     */
    private fun generateConfigMethodForProperty(property: PropertyAnalysis): String {
        val propertyName = property.name
        val returnTypeString =
            typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
        val capitalizedName = propertyName.replaceFirstChar { it.titlecase() }

        return buildString {
            // Getter configuration
            appendLine(
                "    fun $propertyName(behavior: () -> $returnTypeString) " +
                    "{ fake.configure$capitalizedName(behavior) }",
            )

            // Setter configuration for mutable properties
            if (property.isMutable) {
                appendLine(
                    "    fun set$capitalizedName(behavior: ($returnTypeString) -> Unit) " +
                        "{ fake.configureSet$capitalizedName(behavior) }",
                )
            }
        }.trimEnd() // Remove trailing newline so caller can control formatting
    }

    /**
     * Generates a single configuration method for a function (abstract or open).
     * Shared logic between interface and class DSL generation.
     */
    private fun generateConfigMethodForFunction(function: FunctionAnalysis): String {
        val functionName = function.name
        val suspendModifier = if (function.isSuspend) "suspend " else ""

        // Build parameter types list
        val parameterTypes =
            if (function.parameters.isEmpty()) {
                ""
            } else {
                function.parameters.joinToString(", ") { param ->
                    val paramType =
                        if (param.isVararg) {
                            val elementType = unwrapVarargsType(param)
                            "Array<out $elementType>"
                        } else {
                            typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                        }
                    paramType
                }
            }

        // Build return type
        val returnTypeString = typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)

        // Build behavior signature
        val behaviorSignature =
            if (function.parameters.isEmpty()) {
                "$suspendModifier() -> $returnTypeString"
            } else {
                "$suspendModifier($parameterTypes) -> $returnTypeString"
            }

        return "    fun $functionName(behavior: $behaviorSignature) { fake.configure${functionName.replaceFirstChar {
            it.uppercase()
        }}(behavior) }"
    }
}
