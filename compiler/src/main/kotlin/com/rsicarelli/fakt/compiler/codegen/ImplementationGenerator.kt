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

            // Extract just type parameter names (without constraints) for use as type arguments
            val typeParameterNames =
                if (analysis.typeParameters.isNotEmpty()) {
                    analysis.typeParameters.map { it.substringBefore(" :").trim() }
                } else {
                    emptyList()
                }

            // Generate interface name with type parameter names only (no constraints)
            val interfaceWithGenerics =
                if (typeParameterNames.isNotEmpty()) {
                    "${analysis.interfaceName}<${typeParameterNames.joinToString(", ")}>"
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

                val returnTypeString =
                    typeResolver.irTypeToKotlinString(
                        function.returnType,
                        preserveTypeParameters = true,
                    )
                val suspendModifier = if (function.isSuspend) "suspend " else ""

                // Phase 3: For method-level generics, add generic configure method with cast
                if (hasMethodGenerics) {
                    // Extract method type parameter names for conversion
                    val methodTypeParamNames =
                        function.typeParameters
                            .map { it.substringBefore(" :").trim() }
                            .toSet()

                    // Build the cast signature with correct parameter arity
                    val castParamTypes =
                        if (function.parameters.isEmpty()) {
                            ""
                        } else {
                            List(function.parameters.size) { "Any?" }.joinToString(", ")
                        }

                    // Calculate converted return type (preserves wrappers like Result<T>, List<T>)
                    val convertedReturnType =
                        if (containsMethodTypeParam(returnTypeString, methodTypeParamNames)) {
                            convertMethodTypeParamsToAny(returnTypeString, methodTypeParamNames)
                        } else {
                            returnTypeString
                        }

                    appendLine(
                        "    internal fun $methodTypeParams configure${functionName.capitalize()}(" +
                            "behavior: $suspendModifier($parameterTypes) -> $returnTypeString) {",
                    )
                    appendLine("        @Suppress(\"UNCHECKED_CAST\")")
                    appendLine("        ${functionName}Behavior = behavior as $suspendModifier($castParamTypes) -> $convertedReturnType")
                    appendLine("    }")
                } else {
                    appendLine(
                        "    internal fun configure${functionName.capitalize()}(" +
                            "behavior: $suspendModifier($parameterTypes) -> $returnTypeString" +
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
                        "<${function.typeParameters.joinToString(", ")}> "
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
                appendLine("    override ${suspendModifier}fun $methodTypeParams$functionName($parameters): $returnTypeString {")

                // Phase 3: For method-level generics, add @Suppress and cast
                if (function.typeParameters.isNotEmpty()) {
                    // Extract method type parameter names for conversion checking
                    val methodTypeParamNames =
                        function.typeParameters
                            .map { it.substringBefore(" :").trim() }
                            .toSet()

                    // Build parameter list with casts for types containing method-level generics
                    val castedParamNames = function.parameters.joinToString(", ") { param ->
                        val paramTypeString =
                            typeResolver.irTypeToKotlinString(
                                param.type,
                                preserveTypeParameters = true,
                            )
                        // If parameter type contains method-level type parameters, cast to converted type
                        if (containsMethodTypeParam(paramTypeString, methodTypeParamNames)) {
                            val convertedType = convertMethodTypeParamsToAny(paramTypeString, methodTypeParamNames)
                            "${param.name} as $convertedType"
                        } else {
                            param.name
                        }
                    }

                    appendLine("        @Suppress(\"UNCHECKED_CAST\")")
                    appendLine("        return ${functionName}Behavior($castedParamNames) as $returnTypeString")
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
                // Extract just the type parameter names (without constraints) for checking
                val methodTypeParamNames =
                    function.typeParameters
                        .map { it.substringBefore(" :").trim() }
                        .toSet()

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
                                    // If this type contains a method-level type parameter, recursively convert
                                    // This preserves wrappers like Result<T>, List<T> while converting type params to Any?
                                    if (hasMethodGenerics && containsMethodTypeParam(typeString, methodTypeParamNames)) {
                                        convertMethodTypeParamsToAny(typeString, methodTypeParamNames)
                                    } else {
                                        typeString
                                    }
                                }
                            varargsPrefix + paramType
                        }
                    }

                // Use EXACT return type for type safety (or recursively convert method generics)
                // This preserves wrappers like Result<T>, List<T> while converting type params to Any?
                val returnTypeString =
                    typeResolver.irTypeToKotlinString(
                        function.returnType,
                        preserveTypeParameters = true,
                    )
                val returnType =
                    if (hasMethodGenerics && containsMethodTypeParam(returnTypeString, methodTypeParamNames)) {
                        convertMethodTypeParamsToAny(returnTypeString, methodTypeParamNames)
                    } else {
                        returnTypeString
                    }

                // Generate default based on BOTH converted and original types
                // Pass original type to enable smart defaults even when stored as Any?
                val defaultLambda = generateTypeSafeDefault(function, returnType, returnTypeString)

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
     *
     * @param function The function analysis
     * @param convertedType The CONVERTED return type (may be Any? for method-level generics)
     * @param originalType The ORIGINAL return type before conversion (preserves generic info)
     */
    private fun generateTypeSafeDefault(
        function: FunctionAnalysis,
        convertedType: String,
        originalType: String = convertedType,
    ): String {
        // Use original type for pattern matching, converted type for storage
        // This allows smart defaults even when method-level generics are stored as Any?
        val returnType = convertedType
        val typeForDefaultDetection = originalType

        // Check if this is a method-level generic function
        val hasMethodGenerics = function.typeParameters.isNotEmpty()
        val methodTypeParamNames = function.typeParameters.toSet()

        // For method-level generics, check if first param is function returning T
        if (hasMethodGenerics && function.parameters.isNotEmpty()) {
            val firstParamType =
                typeResolver.irTypeToKotlinString(
                    function.parameters[0].type,
                    preserveTypeParameters = true,
                )

            // If first parameter is a function type returning a type parameter (e.g., () -> T)
            // AND the method returns that same type parameter directly,
            // Generate identity behavior: execute the function
            val isExecutableParam =
                methodTypeParamNames.any { typeParam ->
                    firstParamType.matches(Regex(".*\\(.*\\)\\s*->\\s*$typeParam\\b.*")) &&
                        // Return type must be exactly that type parameter (not wrapped like List<T>)
                        typeForDefaultDetection.trim() == typeParam
                }

            if (isExecutableParam) {
                // Generate identity lambda that executes the function parameter
                val paramName = function.parameters[0].name

                // Determine the identity call based on suspend modifier
                val identityCall = if (function.isSuspend) {
                    "($paramName as suspend () -> Any?)()"
                } else {
                    "($paramName as () -> Any?)()"
                }

                // Check if return type is Result<T> and wrap accordingly
                val wrappedCall = if (typeForDefaultDetection.startsWith("Result<")) {
                    "Result.success($identityCall)"
                } else {
                    identityCall
                }

                // Generate lambda with proper parameter list
                return if (function.parameters.size == 1) {
                    "{ $paramName -> $wrappedCall }"
                } else {
                    // Multiple params: execute first one, ignore others
                    val params =
                        function.parameters
                            .mapIndexed { idx, p ->
                                if (idx == 0) p.name else "_"
                            }.joinToString(", ")
                    "{ $params -> $wrappedCall }"
                }
            }
        }

        // Check for identity function pattern: fun process(item: T): T
        // Single parameter with same type as return type → identity function
        if (function.parameters.size == 1 && !hasMethodGenerics) {
            val paramType =
                typeResolver.irTypeToKotlinString(
                    function.parameters[0].type,
                    preserveTypeParameters = true,
                )

            // If param type matches return type exactly, it's an identity function
            if (paramType == returnType) {
                return "{ it }"
            }
        }

        // Fallback to stdlib defaults using ORIGINAL type for smart detection
        // Pass BOTH types: original for pattern matching, converted for type arguments
        val defaultValue = generateKotlinStdlibDefault(typeForDefaultDetection, returnType)

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
     *
     * @param originalType The original type for pattern matching (e.g., "Map<K, String>")
     * @param convertedType The converted storage type (e.g., "Any?" for method-level generics)
     */
    private fun generateKotlinStdlibDefault(
        originalType: String,
        convertedType: String = originalType
    ): String =
        // Check nullable types FIRST - they always default to null
        if (originalType.endsWith("?")) {
            "null"
        } else {
            getPrimitiveDefaults(originalType)
                ?: getCollectionDefaults(originalType, convertedType)
                ?: getKotlinStdlibDefaults(originalType, convertedType)
                ?: handleDomainType(originalType)
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
     * @param originalType The original type for pattern matching
     * @param convertedType The converted storage type
     * @return Default value or null if not a collection
     */
    private fun getCollectionDefaults(
        originalType: String,
        convertedType: String = originalType
    ): String? =
        getPrimitiveArrayDefault(originalType)
            ?: when {
                // Lists
                originalType.startsWith("List<") -> extractAndCreateCollection(convertedType, "emptyList")
                originalType.startsWith("MutableList<") -> extractAndCreateCollection(convertedType, "mutableListOf")
                originalType.startsWith("Collection<") -> extractAndCreateCollection(convertedType, "emptyList")
                originalType.startsWith("Iterable<") -> extractAndCreateCollection(convertedType, "emptyList")

                // Sets
                originalType.startsWith("Set<") -> extractAndCreateCollection(convertedType, "emptySet")
                originalType.startsWith("MutableSet<") -> extractAndCreateCollection(convertedType, "mutableSetOf")

                // Maps
                originalType.startsWith("Map<") -> extractAndCreateMap(convertedType, "emptyMap")
                originalType.startsWith("MutableMap<") -> extractAndCreateMap(convertedType, "mutableMapOf")

                // Arrays
                originalType.startsWith("Array<") -> extractAndCreateArray(originalType)

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
     * @param originalType The original type for pattern matching
     * @param convertedType The converted storage type
     * @return Default value or null if not a stdlib type
     */
    private fun getKotlinStdlibDefaults(
        originalType: String,
        convertedType: String = originalType
    ): String? =
        when {
            originalType.startsWith("Result<") -> extractAndCreateResult(originalType, convertedType)
            originalType.startsWith("Sequence<") -> extractAndCreateCollection(convertedType, "emptySequence")
            originalType.endsWith("?") -> "null"
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
        // For Any? (method-level generics), need explicit type argument
        // Otherwise type inference fails: emptyList() -> cannot infer type
        return if (typeString == "Any?") {
            "$constructor<Any?>()"
        } else {
            // Type inference handles nested generics correctly (e.g., List<List<T>>)
            "$constructor()"
        }
    }

    private fun extractAndCreateMap(
        typeString: String,
        constructor: String,
    ): String {
        // For Any? (method-level generics), need explicit type argument
        return if (typeString == "Any?") {
            "$constructor<Any?, Any?>()"
        } else {
            // Type inference handles nested generics correctly (e.g., Map<K, List<V>>)
            "$constructor()"
        }
    }

    private fun extractAndCreateArray(typeString: String): String {
        val typeParam = extractFirstTypeParameter(typeString)
        return "emptyArray<$typeParam>()"
    }

    private fun extractAndCreateResult(
        originalType: String,
        convertedType: String = originalType
    ): String {
        val typeParam = extractFirstTypeParameter(originalType)
        val innerDefault = generateKotlinStdlibDefault(typeParam, convertedType)
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
     * Checks if a type string contains method-level type parameters.
     *
     * @param typeString The type string to check
     * @param methodTypeParamNames Set of method-level type parameter names
     * @return true if the type string contains any method-level type parameters
     */
    private fun containsMethodTypeParam(
        typeString: String,
        methodTypeParamNames: Set<String>
    ): Boolean =
        methodTypeParamNames.any { typeParam ->
            // Check if type parameter appears as a standalone word in the type string
            // This handles cases like "T", "List<T>", "() -> T", etc.
            typeString.contains(Regex("\\b$typeParam\\b"))
        }

    /**
     * Recursively converts method-level type params to Any? while preserving wrapper types.
     * E.g., Result<T> -> Result<Any?>, List<T> -> List<Any?>, T -> Any?
     *
     * @param typeString The type string to convert
     * @param methodTypeParamNames Set of method-level type parameter names
     * @return The converted type string
     */
    private fun convertMethodTypeParamsToAny(
        typeString: String,
        methodTypeParamNames: Set<String>
    ): String {
        // Handle Result<T> -> Result<Any?>
        if (typeString.startsWith("Result<")) {
            val innerType = extractFirstTypeParameter(typeString)
            val convertedInner = if (containsMethodTypeParam(innerType, methodTypeParamNames)) {
                convertMethodTypeParamsToAny(innerType, methodTypeParamNames)
            } else {
                innerType
            }
            return "Result<$convertedInner>"
        }

        // Handle List/Set/Collection<T> -> List/Set/Collection<Any?>
        val collectionPrefixes = listOf("List<", "MutableList<", "Set<", "MutableSet<", "Collection<", "Iterable<")
        for (prefix in collectionPrefixes) {
            if (typeString.startsWith(prefix)) {
                val innerType = extractFirstTypeParameter(typeString)
                val convertedInner = if (containsMethodTypeParam(innerType, methodTypeParamNames)) "Any?" else innerType
                return "$prefix$convertedInner>"
            }
        }

        // Handle Map<K, V> -> Map<Any?, Any?>
        if (typeString.startsWith("Map<") || typeString.startsWith("MutableMap<")) {
            val prefix = if (typeString.startsWith("MutableMap<")) "MutableMap<" else "Map<"
            val (key, value) = extractMapTypeParameters(typeString)
            val convertedKey = if (containsMethodTypeParam(key, methodTypeParamNames)) "Any?" else key
            val convertedValue = if (containsMethodTypeParam(value, methodTypeParamNames)) "Any?" else value
            return "$prefix$convertedKey, $convertedValue>"
        }

        // Pure type parameter -> Any?
        return if (containsMethodTypeParam(typeString, methodTypeParamNames)) "Any?" else typeString
    }

    /**
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }
}
