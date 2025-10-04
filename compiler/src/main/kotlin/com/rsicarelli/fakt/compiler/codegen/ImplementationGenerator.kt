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
            // Phase 2: Generate generic class declaration with type parameters
            val typeParameters =
                if (analysis.typeParameters.isNotEmpty()) {
                    "<${analysis.typeParameters.joinToString(", ")}>"
                } else {
                    ""
                }

            // Generate interface name with type parameters
            val interfaceWithGenerics =
                if (analysis.typeParameters.isNotEmpty()) {
                    "${analysis.interfaceName}$typeParameters"
                } else {
                    analysis.interfaceName
                }

            appendLine("class $fakeClassName$typeParameters : $interfaceWithGenerics {")

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
                val hasMethodGenerics = function.typeParameters.isNotEmpty()

                // Phase 3: Add method-level type parameters to configure function
                val methodTypeParams =
                    if (hasMethodGenerics) {
                        "<${function.typeParameters.joinToString(", ")}>"
                    } else {
                        ""
                    }

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

                // Phase 3: For method-level generics, add generic configure method with cast
                if (hasMethodGenerics) {
                    // Build the cast signature with correct parameter arity
                    val castParamTypes = if (function.parameters.isEmpty()) {
                        ""
                    } else {
                        List(function.parameters.size) { "Any?" }.joinToString(", ")
                    }

                    appendLine(
                        "    internal fun $methodTypeParams configure${functionName.capitalize()}(" +
                            "behavior: $suspendModifier($parameterTypes) -> $returnType) {",
                    )
                    appendLine("        @Suppress(\"UNCHECKED_CAST\")")
                    appendLine("        ${functionName}Behavior = behavior as $suspendModifier($castParamTypes) -> Any?")
                    appendLine("    }")
                } else {
                    appendLine(
                        "    internal fun configure${functionName.capitalize()}(" +
                            "behavior: $suspendModifier($parameterTypes) -> $returnType" +
                            ") { ${functionName}Behavior = behavior }",
                    )
                }
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

                // Phase 3: Preserve method-level type parameters
                val methodTypeParams =
                    if (function.typeParameters.isNotEmpty()) {
                        "<${function.typeParameters.joinToString(", ")}>"
                    } else {
                        ""
                    }

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
                // Phase 3: Include method-level type parameters
                appendLine("    override ${suspendModifier}fun $methodTypeParams $functionName($parameters): $returnTypeString {")

                // Phase 3: For method-level generics, add @Suppress and cast
                if (function.typeParameters.isNotEmpty()) {
                    appendLine("        @Suppress(\"UNCHECKED_CAST\")")
                    appendLine("        return ${functionName}Behavior($parameterNames) as $returnTypeString")
                } else {
                    appendLine("        return ${functionName}Behavior($parameterNames)")
                }
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

                // Phase 3: Handle method-level generics specially
                // Method-level type parameters cannot be used in instance fields
                // So we use Any? for those and cast at the method level
                val hasMethodGenerics = function.typeParameters.isNotEmpty()
                val methodTypeParamNames = function.typeParameters.toSet()

                // Helper function to check if a type string contains method-level type parameters
                fun containsMethodTypeParam(typeString: String): Boolean {
                    return methodTypeParamNames.any { typeParam ->
                        // Check if type parameter appears as a standalone word in the type string
                        // This handles cases like "T", "List<T>", "() -> T", etc.
                        typeString.contains(Regex("\\b$typeParam\\b"))
                    }
                }

                // Use EXACT parameter types for type safety (or Any? for method generics)
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
                                    val typeString =
                                        typeResolver.irTypeToKotlinString(
                                            param.type,
                                            preserveTypeParameters = true,
                                        )
                                    // If this type contains a method-level type parameter, use Any?
                                    if (hasMethodGenerics && containsMethodTypeParam(typeString)) {
                                        "Any?"
                                    } else {
                                        typeString
                                    }
                                }
                            varargsPrefix + paramType
                        }
                    }

                // Use EXACT return type for type safety (or Any? for method generics)
                val returnTypeString =
                    typeResolver.irTypeToKotlinString(
                        function.returnType,
                        preserveTypeParameters = true,
                    )
                val returnType =
                    if (hasMethodGenerics && containsMethodTypeParam(returnTypeString)) {
                        "Any?"
                    } else {
                        returnTypeString
                    }

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
     *
     * For method-level generics like fun <T> executeStep(step: () -> T): T,
     * generates identity behavior that executes the function parameter.
     *
     * For identity functions like fun process(item: T): T (class-level T),
     * generates identity behavior { it }.
     */
    private fun generateTypeSafeDefault(function: FunctionAnalysis): String {
        val returnType =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)

        // Check if this is a method-level generic function
        val hasMethodGenerics = function.typeParameters.isNotEmpty()
        val methodTypeParamNames = function.typeParameters.toSet()

        // For method-level generics, check if first param is function returning T
        if (hasMethodGenerics && function.parameters.isNotEmpty()) {
            val firstParamType = typeResolver.irTypeToKotlinString(
                function.parameters[0].type,
                preserveTypeParameters = true
            )

            // If first parameter is a function type returning a type parameter (e.g., () -> T)
            // Generate identity behavior: execute the function
            val isExecutableParam = methodTypeParamNames.any { typeParam ->
                firstParamType.matches(Regex(".*\\(.*\\)\\s*->\\s*$typeParam\\b.*"))
            }

            if (isExecutableParam) {
                // Generate identity lambda that executes the function parameter
                val paramName = function.parameters[0].name
                return if (function.isSuspend) {
                    if (function.parameters.size == 1) {
                        "{ $paramName -> ($paramName as suspend () -> Any?)() }"
                    } else {
                        // Multiple params: execute first one, ignore others
                        val params = function.parameters.mapIndexed { idx, p ->
                            if (idx == 0) p.name else "_"
                        }.joinToString(", ")
                        "{ $params -> ($paramName as suspend () -> Any?)() }"
                    }
                } else {
                    if (function.parameters.size == 1) {
                        "{ $paramName -> ($paramName as () -> Any?)() }"
                    } else {
                        // Multiple params: execute first one, ignore others
                        val params = function.parameters.mapIndexed { idx, p ->
                            if (idx == 0) p.name else "_"
                        }.joinToString(", ")
                        "{ $params -> ($paramName as () -> Any?)() }"
                    }
                }
            }
        }

        // Check for identity function pattern: fun process(item: T): T
        // Single parameter with same type as return type → identity function
        if (function.parameters.size == 1 && !hasMethodGenerics) {
            val paramType = typeResolver.irTypeToKotlinString(
                function.parameters[0].type,
                preserveTypeParameters = true
            )

            // If param type matches return type exactly, it's an identity function
            if (paramType == returnType) {
                return "{ it }"
            }
        }

        // Fallback to stdlib defaults
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
        // Check nullable types FIRST - they always default to null
        if (typeString.endsWith("?")) {
            "null"
        } else {
            getPrimitiveDefaults(typeString)
                ?: getCollectionDefaults(typeString)
                ?: getKotlinStdlibDefaults(typeString)
                ?: handleDomainType(typeString)
        }

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
        // Type inference handles nested generics correctly (e.g., List<List<T>>)
        return "$constructor()"
    }

    private fun extractAndCreateMap(
        typeString: String,
        constructor: String,
    ): String {
        // Type inference handles nested generics correctly (e.g., Map<K, List<V>>)
        return "$constructor()"
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
