// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.compiler.core.types.TypeResolution
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.ParameterAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.PropertyAnalysis

/**
 * Generates configuration DSL classes for fake implementations.
 * Creates type-safe DSL classes that provide convenient configuration of fake behavior.
 */
internal class ConfigurationDslGenerator(
    private val typeResolver: TypeResolution,
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
        val configClassName = "Fake${analysis.interfaceName}Config"
        val (typeParamsForHeader, whereClause) = formatTypeParametersWithWhereClause(analysis.typeParameters)
        val typeParameters = formatTypeParameters(typeParamsForHeader)
        val typeParameterNames = extractTypeParameterNames(analysis.typeParameters)

        return buildString {
            appendLine(
                generateClassHeader(
                    configClassName,
                    typeParameters,
                    fakeClassName,
                    typeParameterNames,
                    whereClause,
                ),
            )
            append(generateFunctionConfigurators(analysis.functions))
            append(generatePropertyConfigurators(analysis.properties))
            append("}")
        }
    }

    private fun generateClassHeader(
        configClassName: String,
        typeParameters: String,
        fakeClassName: String,
        typeParameterNames: String,
        whereClause: String,
    ): String =
        if (whereClause.isNotEmpty()) {
            "class $configClassName$typeParameters(" +
                "private val fake: $fakeClassName$typeParameterNames) where $whereClause {"
        } else {
            "class $configClassName$typeParameters(private val fake: $fakeClassName$typeParameterNames) {"
        }

    private fun generateFunctionConfigurators(functions: List<FunctionAnalysis>): String =
        functions.joinToString("") { function ->
            // Preserve full generic signatures (no erasure)
            val hasMethodGenerics = function.typeParameters.isNotEmpty()

            val methodTypeParams =
                if (hasMethodGenerics) {
                    "<${function.typeParameters.joinToString(", ")}> "
                } else {
                    ""
                }

            // Keep original parameter types (including method-level generics)
            val regularParamTypes =
                if (function.parameters.isEmpty()) {
                    ""
                } else {
                    function.parameters.joinToString(", ") { param ->
                        if (param.isVararg) {
                            val elementType = unwrapVarargsType(param)
                            "Array<out $elementType>"
                        } else {
                            // Keep full signature - no erasure!
                            typeResolver.irTypeToKotlinString(
                                param.type,
                                preserveTypeParameters = true,
                            )
                        }
                    }
                }

            // For extension functions, prepend receiver type to parameter list
            val parameterTypes =
                if (function.extensionReceiverType != null) {
                    val receiverTypeStr = typeResolver.irTypeToKotlinString(function.extensionReceiverType, preserveTypeParameters = true)
                    if (regularParamTypes.isEmpty()) {
                        receiverTypeStr
                    } else {
                        "$receiverTypeStr, $regularParamTypes"
                    }
                } else {
                    regularParamTypes
                }

            // Keep original return type (including method-level generics)
            val returnType =
                typeResolver.irTypeToKotlinString(
                    function.returnType,
                    preserveTypeParameters = true,
                )

            val suspendModifier = if (function.isSuspend) "suspend " else ""

            "    fun $methodTypeParams${function.name}(" +
                "behavior: $suspendModifier($parameterTypes) -> $returnType) " +
                "{ fake.configure${function.name.capitalize()}(behavior) }\n"
        }

    private fun generatePropertyConfigurators(properties: List<PropertyAnalysis>): String =
        properties.joinToString("") { property ->
            val propertyType =
                typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)

            buildString {
                append(
                    "    fun ${property.name}(behavior: () -> $propertyType) " +
                        "{ fake.configure${property.name.capitalize()}(behavior) }\n",
                )

                // For mutable properties, add setter configuration
                if (property.isMutable) {
                    append(
                        "    fun set${property.name.capitalize()}(behavior: ($propertyType) -> Unit) " +
                            "{ fake.configureSet${property.name.capitalize()}(behavior) }\n",
                    )
                }
            }
        }

    private fun formatTypeParameters(typeParamsForHeader: List<String>): String =
        if (typeParamsForHeader.isNotEmpty()) {
            "<${typeParamsForHeader.joinToString(", ")}>"
        } else {
            ""
        }

    private fun extractTypeParameterNames(typeParameters: List<String>): String =
        if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString(", ") { it.substringBefore(" :").trim() }}>"
        } else {
            ""
        }

    /**
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }

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
     *
     * Preserves full generic signatures (no erasure) for type-safe DSL.
     */
    private fun generateConfigMethodForFunction(function: FunctionAnalysis): String {
        val functionName = function.name
        val suspendModifier = if (function.isSuspend) "suspend " else ""

        // Preserve full generic signatures (no erasure)
        val hasMethodGenerics = function.typeParameters.isNotEmpty()

        val methodTypeParams =
            if (hasMethodGenerics) {
                "<${function.typeParameters.joinToString(", ")}> "
            } else {
                ""
            }

        // Keep original parameter types (including method-level generics)
        val regularParamTypes =
            function.parameters.joinToString(", ") { param ->
                if (param.isVararg) {
                    val elementType = unwrapVarargsType(param)
                    "Array<out $elementType>"
                } else {
                    // Keep full signature - no erasure!
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                }
            }

        // For extension functions, prepend receiver type to parameter list
        val parameterTypes =
            if (function.extensionReceiverType != null) {
                val receiverTypeStr = typeResolver.irTypeToKotlinString(function.extensionReceiverType, preserveTypeParameters = true)
                if (regularParamTypes.isEmpty()) {
                    receiverTypeStr
                } else {
                    "$receiverTypeStr, $regularParamTypes"
                }
            } else {
                regularParamTypes
            }

        // Keep original return type (including method-level generics)
        val returnType =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)

        // Build behavior signature with full generic types
        val behaviorSignature =
            if (parameterTypes.isEmpty()) {
                "$suspendModifier() -> $returnType"
            } else {
                "$suspendModifier($parameterTypes) -> $returnType"
            }

        return "    fun $methodTypeParams$functionName(behavior: $behaviorSignature) { fake.configure${
            functionName.replaceFirstChar {
                it.uppercase()
            }
        }(behavior) }"
    }

    /**
     * Check if a type string contains a method-level type parameter.
     */
    private fun containsMethodTypeParam(
        typeString: String,
        methodTypeParamNames: Set<String>,
    ): Boolean =
        methodTypeParamNames.any { typeParam ->
            // Check if type parameter appears as a standalone word in the type string
            typeString.contains(Regex("\\b$typeParam\\b"))
        }

    /**
     * Recursively converts method-level type params to Any? while preserving wrapper types.
     *
     * E.g., Result<T> -> Result<Any?>, List<T> -> List<Any?>, T -> Any?
     */
    private fun convertMethodTypeParamsToAny(
        typeString: String,
        methodTypeParamNames: Set<String>,
    ): String =
        when {
            typeString.startsWith("Result<") -> convertResultType(typeString, methodTypeParamNames)
            typeString.startsWith("Map<") || typeString.startsWith("MutableMap<") ->
                convertMapType(typeString, methodTypeParamNames)

            else -> convertCollectionOrPrimitiveType(typeString, methodTypeParamNames)
        }

    private fun convertResultType(
        typeString: String,
        methodTypeParamNames: Set<String>,
    ): String {
        val innerType = extractFirstTypeParameter(typeString)
        val convertedInner =
            if (containsMethodTypeParam(innerType, methodTypeParamNames)) {
                convertMethodTypeParamsToAny(innerType, methodTypeParamNames)
            } else {
                innerType
            }
        return "Result<$convertedInner>"
    }

    private fun convertMapType(
        typeString: String,
        methodTypeParamNames: Set<String>,
    ): String {
        val prefix = if (typeString.startsWith("MutableMap<")) "MutableMap<" else "Map<"
        val (key, value) = extractMapTypeParameters(typeString)
        val convertedKey = if (containsMethodTypeParam(key, methodTypeParamNames)) "Any?" else key
        val convertedValue =
            if (containsMethodTypeParam(value, methodTypeParamNames)) "Any?" else value
        return "$prefix$convertedKey, $convertedValue>"
    }

    private fun convertCollectionOrPrimitiveType(
        typeString: String,
        methodTypeParamNames: Set<String>,
    ): String {
        val collectionPrefixes =
            listOf("List<", "MutableList<", "Set<", "MutableSet<", "Collection<", "Iterable<")
        collectionPrefixes.forEach { prefix ->
            if (typeString.startsWith(prefix)) {
                val innerType = extractFirstTypeParameter(typeString)
                val convertedInner =
                    if (containsMethodTypeParam(
                            innerType,
                            methodTypeParamNames,
                        )
                    ) {
                        "Any?"
                    } else {
                        innerType
                    }
                return "$prefix$convertedInner>"
            }
        }
        return if (containsMethodTypeParam(typeString, methodTypeParamNames)) "Any?" else typeString
    }

    private fun extractFirstTypeParameter(typeString: String): String {
        val start = typeString.indexOf('<') + 1
        var depth = 1
        var end = start
        while (end < typeString.length && depth > 0) {
            when (typeString[end]) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 1) break
            }
            if (depth > 0) end++
        }
        return typeString.substring(start, end).trim()
    }

    private fun extractMapTypeParameters(typeString: String): Pair<String, String> {
        val start = typeString.indexOf('<') + 1
        val parts = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()

        for (i in start until typeString.length) {
            when (typeString[i]) {
                '<' -> {
                    depth++
                    current.append(typeString[i])
                }

                '>' -> {
                    if (depth == 0) break
                    depth--
                    current.append(typeString[i])
                }

                ',' -> {
                    if (depth == 0) {
                        parts.add(current.toString().trim())
                        current = StringBuilder()
                    } else {
                        current.append(typeString[i])
                    }
                }

                else -> current.append(typeString[i])
            }
        }
        if (current.isNotEmpty()) {
            parts.add(current.toString().trim())
        }

        return parts[0] to parts[1]
    }
}
