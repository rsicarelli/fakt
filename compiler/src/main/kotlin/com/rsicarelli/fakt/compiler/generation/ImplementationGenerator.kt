// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.generation

import com.rsicarelli.fakt.compiler.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.types.TypeResolver
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.isAny

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
        // Regex patterns for generic type detection (moved outside class for performance)
        private val GENERIC_TYPE_PATTERN = Regex(".*\\b[A-Z]\\b.*")
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
                                    // For varargs, unwrap Array<T> to T
                                    val arrayType = typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                                    val unwrappedType =
                                        if (arrayType.startsWith("Array<") && arrayType.endsWith(">")) {
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

                // Use EXACT return type for type safety
                val returnType = typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
                val defaultLambda = generateTypeSafeDefault(function, analysis)

                val suspendModifier = if (function.isSuspend) "suspend " else ""
                appendLine("    private var ${functionName}Behavior: $suspendModifier($parameterTypes) -> $returnType = $defaultLambda")
            }

            // Generate behavior fields for properties (TYPE-SAFE: Use exact types)
            for (property in analysis.properties) {
                val propertyName = property.name
                val propertyType = typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
                val defaultLambda = generateTypeSafePropertyDefault(property, analysis)
                appendLine("    private var ${propertyName}Behavior: () -> $propertyType = $defaultLambda")
            }

            appendLine()

            // Generate method implementations (TYPE-SAFE: No casting needed!)
            for (function in analysis.functions) {
                val functionName = function.name

                // Preserve EXACT method signature from interface
                val returnTypeString = typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
                val parameters =
                    function.parameters.joinToString(", ") { param ->
                        val varargsPrefix = if (param.isVararg) "vararg " else ""
                        val paramType =
                            if (param.isVararg) {
                                // For varargs, unwrap Array<T> to T
                                val arrayType = typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                                val unwrappedType =
                                    if (arrayType.startsWith("Array<") && arrayType.endsWith(">")) {
                                        arrayType.substring(6, arrayType.length - 1) // Extract T from Array<T>
                                    } else {
                                        "String" // Safe fallback for varargs
                                    }
                                unwrappedType
                            } else {
                                typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
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
                val returnTypeString = typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
                appendLine("    override val $propertyName: $returnTypeString get() {")
                appendLine("        return ${propertyName}Behavior()")
                appendLine("    }")
            }

            appendLine()

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
                                    // For varargs, unwrap Array<T> to T
                                    val arrayType = typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                                    val unwrappedType =
                                        if (arrayType.startsWith("Array<") && arrayType.endsWith(">")) {
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
                appendLine(
                    "    internal fun configure${functionName.capitalize()}(behavior: $suspendModifier($parameterTypes) -> $returnType) { ${functionName}Behavior = behavior }",
                )
            }

            // Generate configuration methods for properties (TYPE-SAFE: Use exact types)
            for (property in analysis.properties) {
                val propertyName = property.name
                val propertyType = typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
                appendLine(
                    "    internal fun configure${propertyName.capitalize()}(behavior: () -> $propertyType) { ${propertyName}Behavior = behavior }",
                )
            }

            append("}")
        }

    /**
     * Substitute interface-level type parameters with Any for NoGenerics pattern.
     * Example: TKey -> Any, TValue -> Any, but preserve method-level generics like T, R
     */
    private fun substituteInterfaceTypeParameters(
        typeString: String,
        interfaceTypeParams: List<String>,
    ): String {
        var result = typeString
        for (typeParam in interfaceTypeParams) {
            // Replace interface-level type parameters with Any
            // Use word boundaries to avoid replacing parts of other words
            result = result.replace("\\b$typeParam\\b".toRegex(), "Any")
        }
        return result
    }

    /**
     * Check if function has generic parameters (type parameters in method or parameter types).
     */
    private fun hasGenericParameters(function: com.rsicarelli.fakt.compiler.analysis.FunctionAnalysis): Boolean {
        // Check for method-level type parameters
        if (function.typeParameters.isNotEmpty()) {
            return true
        }

        // Check if any parameter uses type parameters (simplified check)
        return function.parameters.any { param ->
            val paramType = typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
            // Simple heuristic: if type contains single letters or known generic patterns, it's likely generic
            paramType.matches(
                GENERIC_TYPE_PATTERN,
            ) || paramType.contains("<") || paramType == "T" || paramType == "K" || paramType == "V" ||
                paramType == "R"
        }
    }

    /**
     * Generate type-safe default for functions.
     * Uses broad Kotlin stdlib support with exact types - no casting!
     */
    private fun generateTypeSafeDefault(
        function: com.rsicarelli.fakt.compiler.analysis.FunctionAnalysis,
        analysis: InterfaceAnalysis,
    ): String {
        val returnType = typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
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
    private fun generateTypeSafePropertyDefault(
        property: com.rsicarelli.fakt.compiler.analysis.PropertyAnalysis,
        analysis: InterfaceAnalysis,
    ): String {
        val propertyType = typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
        val defaultValue = generateKotlinStdlibDefault(propertyType)
        return "{ $defaultValue }"
    }

    /**
     * Generate defaults for Kotlin stdlib types - broad support!
     * This covers all common Kotlin types developers use.
     */
    private fun generateKotlinStdlibDefault(typeString: String): String =
        when {
            // ✅ Primitive types
            typeString == "String" -> "\"\""
            typeString == "Int" -> "0"
            typeString == "Long" -> "0L"
            typeString == "Boolean" -> "false"
            typeString == "Double" -> "0.0"
            typeString == "Float" -> "0.0f"
            typeString == "Byte" -> "0"
            typeString == "Short" -> "0"
            typeString == "Char" -> "'\\u0000'"
            typeString == "Unit" -> "Unit"

            // ✅ Collections (with exact type parameters)
            typeString.startsWith("List<") -> extractAndCreateCollection(typeString, "emptyList")
            typeString.startsWith("MutableList<") -> extractAndCreateCollection(typeString, "mutableListOf")
            typeString.startsWith("Set<") -> extractAndCreateCollection(typeString, "emptySet")
            typeString.startsWith("MutableSet<") -> extractAndCreateCollection(typeString, "mutableSetOf")
            typeString.startsWith("Collection<") -> extractAndCreateCollection(typeString, "emptyList")
            typeString.startsWith("Iterable<") -> extractAndCreateCollection(typeString, "emptyList")

            // ✅ Maps (with exact type parameters)
            typeString.startsWith("Map<") -> extractAndCreateMap(typeString, "emptyMap")
            typeString.startsWith("MutableMap<") -> extractAndCreateMap(typeString, "mutableMapOf")

            // ✅ Arrays
            typeString.startsWith("Array<") -> extractAndCreateArray(typeString)
            typeString == "IntArray" -> "intArrayOf()"
            typeString == "LongArray" -> "longArrayOf()"
            typeString == "DoubleArray" -> "doubleArrayOf()"
            typeString == "FloatArray" -> "floatArrayOf()"
            typeString == "BooleanArray" -> "booleanArrayOf()"
            typeString == "ByteArray" -> "byteArrayOf()"
            typeString == "ShortArray" -> "shortArrayOf()"
            typeString == "CharArray" -> "charArrayOf()"

            // ✅ Result types
            typeString.startsWith("Result<") -> extractAndCreateResult(typeString)

            // ✅ Sequences
            typeString.startsWith("Sequence<") -> extractAndCreateCollection(typeString, "emptySequence")

            // ✅ Nullable types
            typeString.endsWith("?") -> "null"

            // ❌ Domain types (user provides defaults via factory)
            else -> "TODO(\"Provide default for domain type '$typeString' via factory configuration\")"
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
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }
}
