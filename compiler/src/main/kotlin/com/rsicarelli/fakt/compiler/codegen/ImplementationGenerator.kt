// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.ir.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.ParameterAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.PropertyAnalysis
import com.rsicarelli.fakt.compiler.types.TypeResolver
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Generates fake implementation classes.
 * Handles the creation of implementation class code with behavior fields and method implementations.
 *
 * @since 1.0.0
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ImplementationGenerator(
    private val typeResolver: TypeResolver,
) {
    companion object {
        /**
         * Length of "Array<" prefix when extracting generic type from Array<T>.
         */
        private const val ARRAY_PREFIX_LENGTH = 6
    }

    /**
     * Generates the complete implementation class code.
     *
     * @param analysis The analyzed interface metadata
     * @param fakeClassName The name of the fake implementation class
     * @return The generated implementation class code
     */
    fun generateImplementation(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
    ): String =
        buildString {
            // Handle interface-level generics (Option 1: Use Any for type erasure)
            val interfaceWithGenerics =
                if (analysis.typeParameters.isNotEmpty()) {
                    val genericParams = analysis.typeParameters.joinToString(", ") { "Any" }
                    "${analysis.interfaceName}<$genericParams>"
                } else {
                    analysis.interfaceName
                }
            appendLine("class $fakeClassName : $interfaceWithGenerics {")

            // Generate behavior fields for functions and properties
            append(generateBehaviorProperties(analysis))

            appendLine()

            // Generate method and property overrides
            append(generateMethodOverrides(analysis))

            appendLine()

            // Generate configuration methods
            append(generateConfigMethods(analysis))

            append("}")
        }

    /**
     * Generates configuration methods for behavior customization.
     *
     * @param analysis The analyzed interface metadata
     * @return Generated configuration methods code
     */
    private fun generateConfigMethods(analysis: InterfaceAnalysis): String =
        buildString {
            // Generate configuration methods for functions (TYPE-SAFE: Use exact types)
            for (function in analysis.functions) {
                val functionName = function.name

                // Use EXACT parameter types for type-safe configuration
                val parameterTypes =
                    if (function.parameters.isEmpty()) {
                        ""
                    } else {
                        function.parameters.joinToString(", ") { param ->
                            val varargsPrefix = if (param.isVararg) "vararg " else ""
                            val paramType =
                                if (param.isVararg) {
                                    unwrapVarargsType(param)
                                } else {
                                    typeResolver.irTypeToKotlinString(
                                        param.type,
                                        preserveTypeParameters = true,
                                    )
                                }
                            varargsPrefix + paramType
                        }
                    }

                val returnType =
                    typeResolver.irTypeToKotlinString(
                        function.returnType,
                        preserveTypeParameters = true,
                    )
                val suspendModifier = if (function.isSuspend) "suspend " else ""
                appendLine(
                    "    internal fun configure${functionName.capitalize()}(" +
                        "behavior: $suspendModifier($parameterTypes) -> $returnType" +
                        ") { ${functionName}Behavior = behavior }",
                )
            }

            // Generate configuration methods for properties (TYPE-SAFE: Use exact types)
            for (property in analysis.properties) {
                val propertyName = property.name
                val propertyType =
                    typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
                appendLine(
                    "    internal fun configure${propertyName.capitalize()}(" +
                        "behavior: () -> $propertyType) { ${propertyName}Behavior = behavior }",
                )
            }
        }

    /**
     * Generates method and property overrides.
     *
     * @param analysis The analyzed interface metadata
     * @return Generated method/property override code
     */
    private fun generateMethodOverrides(analysis: InterfaceAnalysis): String =
        buildString {
            // Generate method implementations (TYPE-SAFE: No casting needed!)
            for (function in analysis.functions) {
                val functionName = function.name

                // Preserve EXACT method signature from interface
                val returnTypeString =
                    typeResolver.irTypeToKotlinString(
                        function.returnType,
                        preserveTypeParameters = true,
                    )
                val parameters =
                    function.parameters.joinToString(", ") { param ->
                        val varargsPrefix = if (param.isVararg) "vararg " else ""
                        val paramType =
                            if (param.isVararg) {
                                unwrapVarargsType(param)
                            } else {
                                typeResolver.irTypeToKotlinString(
                                    param.type,
                                    preserveTypeParameters = true,
                                )
                            }

                        // Override methods cannot have default values - interface already defines them
                        "$varargsPrefix${param.name}: $paramType"
                    }
                val parameterNames = function.parameters.joinToString(", ") { it.name }

                val suspendModifier = if (function.isSuspend) "suspend " else ""

                // Generate method with NO CASTING - types match exactly!
                appendLine("    override ${suspendModifier}fun $functionName($parameters): $returnTypeString {")
                appendLine("        return ${functionName}Behavior($parameterNames)")
                appendLine("    }")
            }

            // Generate property implementations (TYPE-SAFE: No casting needed!)
            for (property in analysis.properties) {
                val propertyName = property.name
                val returnTypeString =
                    typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
                appendLine("    override val $propertyName: $returnTypeString get() {")
                appendLine("        return ${propertyName}Behavior()")
                appendLine("    }")
            }
        }

    /**
     * Generates private behavior fields that store lambdas for dynamic function/property behavior.
     * Creates type-safe behavior fields with exact parameter and return types.
     *
     * @param analysis The analyzed interface metadata
     * @return Generated behavior properties code
     */
    private fun generateBehaviorProperties(analysis: InterfaceAnalysis): String =
        buildString {
            // Generate behavior fields for functions (TYPE-SAFE: Use exact types)
            for (function in analysis.functions) {
                val functionName = function.name

                // Use EXACT parameter types for type safety
                val parameterTypes =
                    if (function.parameters.isEmpty()) {
                        ""
                    } else {
                        function.parameters.joinToString(", ") { param ->
                            val varargsPrefix = if (param.isVararg) "vararg " else ""
                            val paramType =
                                if (param.isVararg) {
                                    unwrapVarargsType(param)
                                } else {
                                    typeResolver.irTypeToKotlinString(
                                        param.type,
                                        preserveTypeParameters = true,
                                    )
                                }
                            varargsPrefix + paramType
                        }
                    }

                // Use EXACT return type for type safety
                val returnType =
                    typeResolver.irTypeToKotlinString(
                        function.returnType,
                        preserveTypeParameters = true,
                    )
                val defaultLambda = generateTypeSafeDefault(function)

                val suspendModifier = if (function.isSuspend) "suspend " else ""
                appendLine(
                    "    private var ${functionName}Behavior: " +
                        "$suspendModifier($parameterTypes) -> $returnType = $defaultLambda",
                )
            }

            // Generate behavior fields for properties (TYPE-SAFE: Use exact types)
            for (property in analysis.properties) {
                val propertyName = property.name
                val propertyType =
                    typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
                val defaultLambda = generateTypeSafePropertyDefault(property)
                appendLine(
                    "    private var ${propertyName}Behavior: () -> $propertyType = $defaultLambda",
                )
            }
        }

    /**
     * Generate type-safe default for functions.
     * Uses broad Kotlin stdlib support with exact types - no casting!
     */
    private fun generateTypeSafeDefault(function: FunctionAnalysis): String {
        val returnType =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
        val defaultValue = generateKotlinStdlibDefault(returnType)

        return if (function.parameters.isEmpty()) {
            "{ $defaultValue }"
        } else if (function.parameters.size == 1) {
            "{ _ -> $defaultValue }"
        } else {
            "{ ${function.parameters.joinToString(", ") { "_" }} -> $defaultValue }"
        }
    }

    /**
     * Generate type-safe default for properties.
     * Uses broad Kotlin stdlib support with exact types - no casting!
     */
    private fun generateTypeSafePropertyDefault(property: PropertyAnalysis): String {
        val propertyType =
            typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
        val defaultValue = generateKotlinStdlibDefault(propertyType)
        return "{ $defaultValue }"
    }

    /**
     * Generate defaults for Kotlin stdlib types using category-based delegation.
     * Orchestrates primitive, collection, and stdlib default generation.
     */
    private fun generateKotlinStdlibDefault(typeString: String): String =
        getPrimitiveDefaults(typeString)
            ?: getCollectionDefaults(typeString)
            ?: getKotlinStdlibDefaults(typeString)
            ?: handleDomainType(typeString)

    /**
     * Returns default values for Kotlin primitive types.
     *
     * @param typeString The type name to check
     * @return Default value or null if not a primitive
     */
    private fun getPrimitiveDefaults(typeString: String): String? =
        when (typeString) {
            "String" -> "\"\""
            "Int" -> "0"
            "Long" -> "0L"
            "Boolean" -> "false"
            "Double" -> "0.0"
            "Float" -> "0.0f"
            "Byte" -> "0"
            "Short" -> "0"
            "Char" -> "'\\u0000'"
            "Unit" -> "Unit"
            else -> null
        }

    /**
     * Returns default values for Kotlin collection types (List, Set, Map, Array).
     *
     * @param typeString The type name to check
     * @return Default value or null if not a collection
     */
    private fun getCollectionDefaults(typeString: String): String? =
        getPrimitiveArrayDefault(typeString)
            ?: when {
                // Lists
                typeString.startsWith("List<") -> extractAndCreateCollection(typeString, "emptyList")
                typeString.startsWith("MutableList<") -> extractAndCreateCollection(typeString, "mutableListOf")
                typeString.startsWith("Collection<") -> extractAndCreateCollection(typeString, "emptyList")
                typeString.startsWith("Iterable<") -> extractAndCreateCollection(typeString, "emptyList")

                // Sets
                typeString.startsWith("Set<") -> extractAndCreateCollection(typeString, "emptySet")
                typeString.startsWith("MutableSet<") -> extractAndCreateCollection(typeString, "mutableSetOf")

                // Maps
                typeString.startsWith("Map<") -> extractAndCreateMap(typeString, "emptyMap")
                typeString.startsWith("MutableMap<") -> extractAndCreateMap(typeString, "mutableMapOf")

                // Arrays
                typeString.startsWith("Array<") -> extractAndCreateArray(typeString)

                else -> null
            }

    /**
     * Returns default values for primitive array types.
     *
     * @param typeString The type name to check
     * @return Default array constructor or null if not a primitive array
     */
    private fun getPrimitiveArrayDefault(typeString: String): String? =
        when (typeString) {
            "IntArray" -> "intArrayOf()"
            "LongArray" -> "longArrayOf()"
            "DoubleArray" -> "doubleArrayOf()"
            "FloatArray" -> "floatArrayOf()"
            "BooleanArray" -> "booleanArrayOf()"
            "ByteArray" -> "byteArrayOf()"
            "ShortArray" -> "shortArrayOf()"
            "CharArray" -> "charArrayOf()"
            else -> null
        }

    /**
     * Returns default values for Kotlin stdlib types (Result, Sequence, nullable).
     *
     * @param typeString The type name to check
     * @return Default value or null if not a stdlib type
     */
    private fun getKotlinStdlibDefaults(typeString: String): String? =
        when {
            typeString.startsWith("Result<") -> extractAndCreateResult(typeString)
            typeString.startsWith("Sequence<") -> extractAndCreateCollection(typeString, "emptySequence")
            typeString.endsWith("?") -> "null"
            else -> null
        }

    /**
     * Handles domain/user types that don't have stdlib defaults.
     *
     * @param typeString The type name
     * @return Error message prompting user configuration
     */
    private fun handleDomainType(typeString: String): String =
        if (typeString.endsWith("?")) {
            "null" // Nullable types can safely default to null
        } else {
            "error(\"Provide default for non-nullable type '$typeString' via factory configuration\")"
        }

    private fun extractAndCreateCollection(
        typeString: String,
        constructor: String,
    ): String {
        val typeParam = extractFirstTypeParameter(typeString)
        return "$constructor<$typeParam>()"
    }

    private fun extractAndCreateMap(
        typeString: String,
        constructor: String,
    ): String {
        val typeParams = extractMapTypeParameters(typeString)
        return "$constructor<${typeParams.first}, ${typeParams.second}>()"
    }

    private fun extractAndCreateArray(typeString: String): String {
        val typeParam = extractFirstTypeParameter(typeString)
        return "emptyArray<$typeParam>()"
    }

    private fun extractAndCreateResult(typeString: String): String {
        val typeParam = extractFirstTypeParameter(typeString)
        val innerDefault = generateKotlinStdlibDefault(typeParam)
        return "Result.success($innerDefault)"
    }

    private fun extractFirstTypeParameter(typeString: String): String {
        val start = typeString.indexOf('<') + 1
        val end = typeString.indexOf('>', start)
        return if (start > 0 && end > start) {
            typeString
                .substring(start, end)
                .split(',')
                .first()
                .trim()
        } else {
            "Any"
        }
    }

    private fun extractMapTypeParameters(typeString: String): Pair<String, String> {
        val start = typeString.indexOf('<') + 1
        val end = typeString.lastIndexOf('>')
        return if (start > 0 && end > start) {
            val params = typeString.substring(start, end).split(',')
            val keyType = params.getOrElse(0) { "Any" }.trim()
            val valueType = params.getOrElse(1) { "Any" }.trim()
            keyType to valueType
        } else {
            "Any" to "Any"
        }
    }

    /**
     * Unwraps varargs Array<T> type to element type T for type-safe code generation.
     * Converts `vararg items: Array<String>` → `vararg items: String`.
     *
     * @param param The parameter to unwrap
     * @return The unwrapped element type string
     */
    private fun unwrapVarargsType(param: ParameterAnalysis): String {
        val arrayType =
            typeResolver.irTypeToKotlinString(
                param.type,
                preserveTypeParameters = true,
            )
        return if (arrayType.startsWith("Array<") && arrayType.endsWith(">")) {
            arrayType.substring(ARRAY_PREFIX_LENGTH, arrayType.length - 1) // Extract T from Array<T>
        } else {
            "String" // Safe fallback for varargs
        }
    }

    /**
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }
}
