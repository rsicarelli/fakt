// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.ParameterAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.PropertyAnalysis
import com.rsicarelli.fakt.compiler.types.TypeResolver

/**
 * Generates fake implementation classes.
 * Handles the creation of implementation class code with behavior fields and method implementations.
 */
// LargeClass: Code generator with comprehensive type handling (interfaces, classes, generics, varargs)
// Heavily refactored from monolithic methods to 25+ focused helpers for maintainability
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
     * Generates a fake implementation for a class (abstract or final with open members).
     *
     * Key differences from interface generation:
     * - Extends the class with () constructor call
     * - Abstract methods get error() defaults
     * - Open methods get super.methodName() defaults
     *
     * @param analysis The analyzed class metadata
     * @param fakeClassName The name of the fake implementation class
     * @return The generated fake class code
     */
    fun generateClassFake(
        analysis: ClassAnalysis,
        fakeClassName: String,
    ): String =
        buildString {
            // Format type parameters with where clause for multiple constraints
            val (typeParamsForHeader, whereClause) = formatTypeParametersWithWhereClause(analysis.typeParameters)

            val typeParameters =
                if (typeParamsForHeader.isNotEmpty()) {
                    "<${typeParamsForHeader.joinToString(", ")}>"
                } else {
                    ""
                }

            // Extract type parameter names (without constraints) for parent class reference
            val typeParameterNames =
                if (analysis.typeParameters.isNotEmpty()) {
                    analysis.typeParameters.map { it.substringBefore(" :").trim() }
                } else {
                    emptyList()
                }

            // Generate parent class with type parameters
            val parentClassWithGenerics =
                if (typeParameterNames.isNotEmpty()) {
                    "${analysis.className}<${typeParameterNames.joinToString(", ")}>"
                } else {
                    analysis.className
                }

            // Generate subclass header (extends parent class) with type parameters
            if (whereClause.isNotEmpty()) {
                appendLine("class $fakeClassName$typeParameters : $parentClassWithGenerics() where $whereClause {")
            } else {
                appendLine("class $fakeClassName$typeParameters : $parentClassWithGenerics() {")
            }

            // Generate behavior properties for abstract and open members
            append(generateClassBehaviorProperties(analysis))

            appendLine()

            // Generate call tracking fields
            append(generateClassCallTrackingFields(analysis))

            appendLine()

            // Generate method and property overrides
            append(generateClassMethodOverrides(analysis))

            appendLine()

            // Generate configuration methods
            append(generateClassConfigMethods(analysis))

            append("}")
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
            // Generate generic class declaration with type parameters
            // Handle where clause for multiple constraints on the same type parameter
            val (typeParamsForHeader, whereClause) = formatTypeParametersWithWhereClause(analysis.typeParameters)

            val typeParameters =
                if (typeParamsForHeader.isNotEmpty()) {
                    "<${typeParamsForHeader.joinToString(", ")}>"
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

            // Generate class header with optional where clause
            if (whereClause.isNotEmpty()) {
                appendLine("class $fakeClassName$typeParameters : $interfaceWithGenerics where $whereClause {")
            } else {
                appendLine("class $fakeClassName$typeParameters : $interfaceWithGenerics {")
            }

            // Generate behavior fields for functions and properties
            append(generateBehaviorProperties(analysis))

            appendLine()

            // Generate call tracking fields
            append(generateCallTrackingFields(analysis))

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
            analysis.functions.forEach { function ->
                append(generateFunctionConfigMethod(function))
            }

            analysis.properties.forEach { property ->
                append(generatePropertyConfigMethod(property))
            }
        }

    private fun generateFunctionConfigMethod(function: FunctionAnalysis): String {
        // Build context for method-level generic handling
        val methodTypeContext = buildMethodTypeParamContext(function)
        val hasMethodGenerics = methodTypeContext.hasMethodGenerics

        // Use context-aware parameter type building
        val parameterTypes = buildConfigParameterTypes(function)
        val returnTypeString =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
        val suspendModifier = if (function.isSuspend) "suspend " else ""

        return if (hasMethodGenerics) {
            buildGenericConfigMethod(function, parameterTypes, returnTypeString, suspendModifier)
        } else {
            buildSimpleConfigMethod(function, parameterTypes, returnTypeString, suspendModifier)
        }
    }

    /**
     * Build configuration parameter types preserving full generic signatures.
     *
     * Preserves full generic signatures for type-safe DSL.
     * The wrapper adapter in buildGenericConfigMethod handles the bridge to erased storage.
     *
     * Key improvements:
     * - Preserves ALL type parameters (class-level and method-level)
     * - Handles varargs correctly
     * - Type-safe DSL without developer casting
     *
     * @param function The function analysis
     * @return Parameter types string for configuration method signature
     */
    private fun buildConfigParameterTypes(
        function: FunctionAnalysis,
    ): String {
        if (function.parameters.isEmpty()) return ""

        return function.parameters.joinToString(", ") { param ->
            when {
                param.isVararg -> {
                    val elementType = unwrapVarargsType(param)
                    "Array<out $elementType>"
                }

                else -> {
                    // Preserve full signature - no erasure!
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                }
            }
        }
    }

    private fun buildGenericConfigMethod(
        function: FunctionAnalysis,
        parameterTypes: String,
        returnTypeString: String,
        suspendModifier: String,
    ): String {
        val methodTypeParams = "<${function.typeParameters.joinToString(", ")}>"
        val methodTypeParamNames =
            function.typeParameters.map { it.substringBefore(" :").trim() }.toSet()
        val methodTypeContext = buildMethodTypeParamContext(function)

        // Storage uses erased types - must match buildFunctionParameterTypes logic
        val erasedParamTypes = buildFunctionParameterTypes(function, methodTypeContext)

        val erasedReturnType =
            if (containsMethodTypeParam(returnTypeString, methodTypeParamNames)) {
                convertMethodTypeParamsToAny(returnTypeString, methodTypeParamNames)
            } else {
                returnTypeString
            }

        // Generate wrapper adapter that bridges full signature → erased storage
        return buildString {
            appendLine(
                "    internal fun $methodTypeParams configure${function.name.capitalize()}(" +
                        "behavior: $suspendModifier($parameterTypes) -> $returnTypeString) {",
            )
            appendLine("        @Suppress(\"UNCHECKED_CAST\")")

            // Direct unchecked cast - no wrapper needed for suspend functions
            // The types are compatible at runtime, just different generic parameters
            appendLine(
                "        ${function.name}Behavior = behavior as $suspendModifier($erasedParamTypes) -> $erasedReturnType",
            )

            appendLine("    }")
        }
    }

    private fun buildSimpleConfigMethod(
        function: FunctionAnalysis,
        parameterTypes: String,
        returnTypeString: String,
        suspendModifier: String,
    ): String =
        "    internal fun configure${function.name.capitalize()}(" +
                "behavior: $suspendModifier($parameterTypes) -> $returnTypeString" +
                ") { ${function.name}Behavior = behavior }\n"

    private fun generatePropertyConfigMethod(property: PropertyAnalysis): String {
        val propertyType =
            typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)

        return buildString {
            append(
                "    internal fun configure${property.name.capitalize()}(" +
                        "behavior: () -> $propertyType) { ${property.name}Behavior = behavior }\n",
            )

            // For mutable properties, add setter configuration
            if (property.isMutable) {
                append(
                    "    internal fun configureSet${property.name.capitalize()}(" +
                            "behavior: ($propertyType) -> Unit) { set${property.name.capitalize()}Behavior = behavior }\n",
                )
            }
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
            analysis.functions.forEach { function ->
                append(generateFunctionOverride(function))
            }

            analysis.properties.forEach { property ->
                append(generatePropertyOverride(property))
            }
        }

    private fun generateFunctionOverride(function: FunctionAnalysis): String {
        val methodTypeParams =
            if (function.typeParameters.isNotEmpty()) {
                "<${function.typeParameters.joinToString(", ")}> "
            } else {
                ""
            }

        val returnTypeString =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
        val parameters = buildMethodParameterList(function)
        val suspendModifier = if (function.isSuspend) "suspend " else ""

        val methodCall =
            if (function.typeParameters.isNotEmpty()) {
                buildMethodCallWithCastAndTracking(function, returnTypeString)
            } else {
                val parameterNames = function.parameters.joinToString(", ") { it.name }
                buildString {
                    appendLine("        _${function.name}CallCount.update { it + 1 }")
                    appendLine("        return ${function.name}Behavior($parameterNames)")
                }
            }

        return buildString {
            appendLine(
                "    override ${suspendModifier}fun $methodTypeParams${function.name}(" +
                        "$parameters): $returnTypeString {",
            )
            append(methodCall)
            appendLine("    }")
        }
    }

    private fun buildMethodParameterList(function: FunctionAnalysis): String =
        function.parameters.joinToString(", ") { param ->
            val varargsPrefix = if (param.isVararg) "vararg " else ""
            val paramType =
                if (param.isVararg) {
                    unwrapVarargsType(param)
                } else {
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                }

            // Note: We do NOT add default values in override functions
            // Kotlin rule: overriding functions inherit default values from the interface
            // and cannot redeclare them.

            "$varargsPrefix${param.name}: $paramType"
        }

    private fun buildMethodCallWithCastAndTracking(
        function: FunctionAnalysis,
        returnTypeString: String,
    ): String {
        val methodTypeParamNames =
            function.typeParameters.map { it.substringBefore(" :").trim() }.toSet()

        val castedParamNames =
            function.parameters.joinToString(", ") { param ->
                val paramTypeString =
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                if (containsMethodTypeParam(paramTypeString, methodTypeParamNames)) {
                    val convertedType =
                        convertMethodTypeParamsToAny(paramTypeString, methodTypeParamNames)
                    "${param.name} as $convertedType"
                } else {
                    param.name
                }
            }

        return buildString {
            appendLine("        _${function.name}CallCount.update { it + 1 }")
            appendLine("        @Suppress(\"UNCHECKED_CAST\")")
            appendLine("        return ${function.name}Behavior($castedParamNames) as $returnTypeString")
        }
    }

    private fun generatePropertyOverride(property: PropertyAnalysis): String {
        val returnTypeString =
            typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)

        return if (property.isMutable) {
            buildString {
                appendLine("    override var ${property.name}: $returnTypeString")
                appendLine("        get() {")
                appendLine("            _${property.name}CallCount.update { it + 1 }")
                appendLine("            return ${property.name}Behavior()")
                appendLine("        }")
                appendLine("        set(value) {")
                appendLine("            _set${property.name.capitalize()}CallCount.update { it + 1 }")
                appendLine("            set${property.name.capitalize()}Behavior(value)")
                appendLine("        }")
            }
        } else {
            buildString {
                appendLine("    override val ${property.name}: $returnTypeString get() {")
                appendLine("        _${property.name}CallCount.update { it + 1 }")
                appendLine("        return ${property.name}Behavior()")
                appendLine("    }")
            }
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
            analysis.functions.forEach { function ->
                append(generateFunctionBehaviorProperty(function))
            }

            analysis.properties.forEach { property ->
                append(generatePropertyBehaviorProperty(property))
            }
        }

    /**
     * Generates call tracking fields using MutableStateFlow for thread-safe call counting.
     * For each function and property, creates:
     * - Private MutableStateFlow backing field
     * - Public StateFlow getter for observation
     *
     * @param analysis The analyzed interface metadata
     * @return Generated call tracking fields code
     */
    private fun generateCallTrackingFields(analysis: InterfaceAnalysis): String =
        buildString {
            analysis.functions.forEach { function ->
                appendLine("    private val _${function.name}CallCount = MutableStateFlow(0)")
                appendLine("    val ${function.name}CallCount: StateFlow<Int> get() = _${function.name}CallCount")
            }

            analysis.properties.forEach { property ->
                appendLine("    private val _${property.name}CallCount = MutableStateFlow(0)")
                appendLine("    val ${property.name}CallCount: StateFlow<Int> get() = _${property.name}CallCount")

                // Separate tracking for mutable property setters
                if (property.isMutable) {
                    appendLine(
                        "    private val _set${property.name.capitalize()}CallCount = MutableStateFlow(0)",
                    )
                    appendLine(
                        "    val set${property.name.capitalize()}CallCount: StateFlow<Int> get() = " +
                                "_set${property.name.capitalize()}CallCount",
                    )
                }
            }
        }

    private fun generateFunctionBehaviorProperty(function: FunctionAnalysis): String {
        val methodTypeContext = buildMethodTypeParamContext(function)
        val parameterTypes = buildFunctionParameterTypes(function, methodTypeContext)
        val returnType = buildFunctionReturnType(function, methodTypeContext)
        val defaultLambda =
            generateTypeSafeDefault(function, returnType.converted, returnType.original)

        val suspendModifier = if (function.isSuspend) "suspend " else ""
        return "    private var ${function.name}Behavior: " +
                "$suspendModifier($parameterTypes) -> ${returnType.converted} = $defaultLambda\n"
    }

    private fun generatePropertyBehaviorProperty(property: PropertyAnalysis): String {
        val propertyType =
            typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
        val defaultLambda = generateTypeSafePropertyDefault(property)

        return buildString {
            append("    private var ${property.name}Behavior: () -> $propertyType = $defaultLambda\n")

            // For mutable properties, add setter behavior
            if (property.isMutable) {
                append("    private var set${property.name.capitalize()}Behavior: ($propertyType) -> Unit = { _ -> }\n")
            }
        }
    }

    private data class MethodTypeContext(
        val hasMethodGenerics: Boolean,
        val methodTypeParamNames: Set<String>,
    )

    private data class ConvertedType(
        val converted: String,
        val original: String,
    )

    private fun buildMethodTypeParamContext(function: FunctionAnalysis): MethodTypeContext {
        val hasMethodGenerics = function.typeParameters.isNotEmpty()
        val methodTypeParamNames =
            function.typeParameters
                .map { it.substringBefore(" :").trim() }
                .toSet()
        return MethodTypeContext(hasMethodGenerics, methodTypeParamNames)
    }

    private fun buildFunctionParameterTypes(
        function: FunctionAnalysis,
        context: MethodTypeContext,
    ): String {
        if (function.parameters.isEmpty()) return ""

        return function.parameters.joinToString(", ") { param ->
            when {
                param.isVararg -> {
                    val elementType = unwrapVarargsType(param)
                    "Array<out $elementType>"
                }

                else -> {
                    val typeString =
                        typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                    if (context.hasMethodGenerics &&
                        containsMethodTypeParam(typeString, context.methodTypeParamNames)
                    ) {
                        convertMethodTypeParamsToAny(typeString, context.methodTypeParamNames)
                    } else {
                        typeString
                    }
                }
            }
        }
    }

    private fun buildFunctionReturnType(
        function: FunctionAnalysis,
        context: MethodTypeContext,
    ): ConvertedType {
        val original =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
        val converted =
            if (context.hasMethodGenerics && containsMethodTypeParam(
                    original,
                    context.methodTypeParamNames
                )
            ) {
                convertMethodTypeParamsToAny(original, context.methodTypeParamNames)
            } else {
                original
            }
        return ConvertedType(converted, original)
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
        val returnType = convertedType
        val typeForDefaultDetection = originalType
        val hasMethodGenerics = function.typeParameters.isNotEmpty()

        val lambdaBody =
            when {
                // Case 1: Method-level generic with executable parameter
                hasMethodGenerics && function.parameters.isNotEmpty() &&
                        isExecutableParameter(function, typeForDefaultDetection) -> {
                    generateExecutableParameterCall(function, typeForDefaultDetection)
                }
                // Case 2: Identity function (single param, same type as return)
                function.parameters.size == 1 && !hasMethodGenerics &&
                        isIdentityFunction(function, returnType) -> "it"
                // Case 3: Default stdlib value
                else -> generateKotlinStdlibDefault(typeForDefaultDetection, returnType)
            }

        return wrapInLambda(lambdaBody, function.parameters)
    }

    private fun isExecutableParameter(
        function: FunctionAnalysis,
        typeForDefaultDetection: String,
    ): Boolean {
        val firstParamType =
            typeResolver.irTypeToKotlinString(
                function.parameters[0].type,
                preserveTypeParameters = true,
            )
        // Extract just the type parameter names (without bounds like "T : Comparable<T>")
        val methodTypeParamNames =
            function.typeParameters
                .map { it.substringBefore(" :").trim() }
                .toSet()

        return methodTypeParamNames.any { typeParam ->
            firstParamType.matches(Regex(".*\\(.*\\)\\s*->\\s*$typeParam\\b.*")) &&
                    typeForDefaultDetection.trim() == typeParam
        }
    }

    private fun generateExecutableParameterCall(
        function: FunctionAnalysis,
        typeForDefaultDetection: String,
    ): String {
        val paramName = function.parameters[0].name
        val identityCall =
            if (function.isSuspend) {
                "($paramName as suspend () -> Any?)()"
            } else {
                "($paramName as () -> Any?)()"
            }

        return if (typeForDefaultDetection.startsWith("Result<")) {
            "Result.success($identityCall)"
        } else {
            identityCall
        }
    }

    private fun isIdentityFunction(
        function: FunctionAnalysis,
        returnType: String,
    ): Boolean {
        val paramType =
            typeResolver.irTypeToKotlinString(
                function.parameters[0].type,
                preserveTypeParameters = true,
            )
        return paramType == returnType
    }

    private fun wrapInLambda(
        body: String,
        parameters: List<ParameterAnalysis>,
    ): String =
        when {
            parameters.isEmpty() -> "{ $body }"
            parameters.size == 1 -> {
                when {
                    // Identity function: { it }
                    body == "it" -> "{ it }"
                    // Body uses parameter name: { paramName -> body }
                    body.contains(parameters[0].name) -> "{ ${parameters[0].name} -> $body }"
                    // Body doesn't use parameter: { _ -> body }
                    else -> "{ _ -> $body }"
                }
            }

            else -> {
                val params =
                    parameters
                        .mapIndexed { idx, p ->
                            if (body.contains(p.name)) p.name else "_"
                        }.joinToString(", ")
                "{ $params -> $body }"
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
        convertedType: String = originalType,
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
        convertedType: String = originalType,
    ): String? =
        getPrimitiveArrayDefault(originalType)
            ?: when {
                // Lists
                originalType.startsWith("List<") -> extractAndCreateCollection(
                    convertedType,
                    "emptyList"
                )

                originalType.startsWith("MutableList<") -> extractAndCreateCollection(
                    convertedType,
                    "mutableListOf"
                )

                originalType.startsWith("Collection<") -> extractAndCreateCollection(
                    convertedType,
                    "emptyList"
                )

                originalType.startsWith("Iterable<") -> extractAndCreateCollection(
                    convertedType,
                    "emptyList"
                )

                // Sets
                originalType.startsWith("Set<") -> extractAndCreateCollection(
                    convertedType,
                    "emptySet"
                )

                originalType.startsWith("MutableSet<") -> extractAndCreateCollection(
                    convertedType,
                    "mutableSetOf"
                )

                // Maps
                originalType.startsWith("Map<") -> extractAndCreateMap(convertedType, "emptyMap")
                originalType.startsWith("MutableMap<") -> extractAndCreateMap(
                    convertedType,
                    "mutableMapOf"
                )

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
        convertedType: String = originalType,
    ): String? =
        when {
            originalType.startsWith("Result<") -> extractAndCreateResult(
                originalType,
                convertedType
            )

            originalType.startsWith("Sequence<") -> extractAndCreateCollection(
                convertedType,
                "emptySequence"
            )

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
            // Explicit 'as Nothing' cast required for proper lambda type inference
            // Without it, Kotlin infers { error(...) } as () -> Unit instead of () -> T
            "error(\"Provide default for non-nullable type '$typeString' via factory configuration\") as Nothing"
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
        convertedType: String = originalType,
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
            arrayType.substring(
                ARRAY_PREFIX_LENGTH,
                arrayType.length - 1
            ) // Extract T from Array<T>
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
        methodTypeParamNames: Set<String>,
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
                val convertedInner = if (containsMethodTypeParam(
                        innerType,
                        methodTypeParamNames
                    )
                ) "Any?" else innerType
                return "$prefix$convertedInner>"
            }
        }
        return if (containsMethodTypeParam(typeString, methodTypeParamNames)) "Any?" else typeString
    }

    /**
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }

    /**
     * Formats type parameters for class headers, handling where clauses for multiple constraints.
     *
     * Examples:
     * - ["T"] → (["T"], "")
     * - ["T : Comparable<T>"] → (["T : Comparable<T>"], "")
     * - ["T : CharSequence, Comparable<T>"] → (["T"], "T : CharSequence, T : Comparable<T>")
     *
     * @param typeParameters List of type parameter strings from InterfaceAnalysis
     * @return Pair of (type parameters for header, where clause content)
     */
    private fun formatTypeParametersWithWhereClause(typeParameters: List<String>): Pair<List<String>, String> {
        if (typeParameters.isEmpty()) {
            return emptyList<String>() to ""
        }

        val paramsForHeader = mutableListOf<String>()
        val whereClauses = mutableListOf<String>()

        for (typeParam in typeParameters) {
            // Check if this type parameter has multiple constraints (contains comma after colon)
            val colonIndex = typeParam.indexOf(" :")
            if (colonIndex == -1) {
                // No constraints - just the name
                paramsForHeader.add(typeParam)
                continue
            }

            val name = typeParam.substring(0, colonIndex).trim()
            val constraints = typeParam.substring(colonIndex + 2).trim() // +2 to skip " :"

            // Split constraints by comma
            val constraintList = constraints.split(",").map { it.trim() }

            if (constraintList.size == 1) {
                // Single constraint - can stay in header
                paramsForHeader.add(typeParam)
            } else {
                // Multiple constraints - need where clause
                paramsForHeader.add(name)
                // Add each constraint as a separate where clause: T : CharSequence, T : Comparable<T>
                constraintList.forEach { constraint ->
                    whereClauses.add("$name : $constraint")
                }
            }
        }

        return paramsForHeader to whereClauses.joinToString(", ")
    }

    /**
     * Generates call tracking fields for class members using MutableStateFlow.
     */
    private fun generateClassCallTrackingFields(analysis: ClassAnalysis): String =
        buildString {
            // Track abstract methods
            analysis.abstractMethods.forEach { function ->
                appendLine("    private val _${function.name}CallCount = MutableStateFlow(0)")
                appendLine("    val ${function.name}CallCount: StateFlow<Int> get() = _${function.name}CallCount")
            }

            // Track open methods
            analysis.openMethods.forEach { function ->
                appendLine("    private val _${function.name}CallCount = MutableStateFlow(0)")
                appendLine("    val ${function.name}CallCount: StateFlow<Int> get() = _${function.name}CallCount")
            }

            // Track abstract properties
            analysis.abstractProperties.forEach { property ->
                appendLine("    private val _${property.name}CallCount = MutableStateFlow(0)")
                appendLine("    val ${property.name}CallCount: StateFlow<Int> get() = _${property.name}CallCount")

                if (property.isMutable) {
                    appendLine(
                        "    private val _set${property.name.capitalize()}CallCount = MutableStateFlow(0)",
                    )
                    appendLine(
                        "    val set${property.name.capitalize()}CallCount: StateFlow<Int> get() = " +
                                "_set${property.name.capitalize()}CallCount",
                    )
                }
            }

            // Track open properties
            analysis.openProperties.forEach { property ->
                appendLine("    private val _${property.name}CallCount = MutableStateFlow(0)")
                appendLine("    val ${property.name}CallCount: StateFlow<Int> get() = _${property.name}CallCount")

                if (property.isMutable) {
                    appendLine(
                        "    private val _set${property.name.capitalize()}CallCount = MutableStateFlow(0)",
                    )
                    appendLine(
                        "    val set${property.name.capitalize()}CallCount: StateFlow<Int> get() = " +
                                "_set${property.name.capitalize()}CallCount",
                    )
                }
            }
        }

    /**
     * Generates behavior properties for class members.
     * Abstract members get error() defaults, open members get super call defaults.
     */
    private fun generateClassBehaviorProperties(analysis: ClassAnalysis): String =
        buildString {
            append(generateAbstractMethodBehaviors(analysis.abstractMethods))
            append(generateOpenMethodBehaviors(analysis.openMethods))
            append(generateAbstractPropertyBehaviors(analysis.abstractProperties))
            append(generateOpenPropertyBehaviors(analysis.openProperties))
        }

    private fun generateAbstractMethodBehaviors(methods: List<FunctionAnalysis>): String =
        methods.joinToString("") { function ->
            val parameterTypes = buildMethodParameterTypes(function.parameters)
            val returnTypeString = typeResolver.irTypeToKotlinString(
                function.returnType,
                preserveTypeParameters = true
            )
            val suspendModifier = if (function.isSuspend) "suspend " else ""
            val defaultLambda = createErrorLambda(function.name, function.parameters.size)

            "    private var ${function.name}Behavior: " +
                    "$suspendModifier($parameterTypes) -> $returnTypeString = $defaultLambda\n"
        }

    private fun generateOpenMethodBehaviors(methods: List<FunctionAnalysis>): String =
        methods.joinToString("") { function ->
            val parameterTypes = buildMethodParameterTypes(function.parameters)
            val returnTypeString = typeResolver.irTypeToKotlinString(
                function.returnType,
                preserveTypeParameters = true
            )
            val suspendModifier = if (function.isSuspend) "suspend " else ""
            val defaultLambda = createSuperCallLambda(function)

            "    private var ${function.name}Behavior: " +
                    "$suspendModifier($parameterTypes) -> $returnTypeString = $defaultLambda\n"
        }

    private fun generateAbstractPropertyBehaviors(properties: List<PropertyAnalysis>): String =
        properties.joinToString("") { property ->
            val returnTypeString =
                typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
            val getter =
                "    private var ${property.name}Behavior: () -> $returnTypeString = " +
                        "{ error(\"Configure ${property.name} behavior\") }\n"

            val setter =
                if (property.isMutable) {
                    "    private var set${property.name.capitalize()}Behavior: " +
                            "($returnTypeString) -> Unit = { _ -> error(\"Configure ${property.name} setter\") }\n"
                } else {
                    ""
                }
            getter + setter
        }

    private fun generateOpenPropertyBehaviors(properties: List<PropertyAnalysis>): String =
        properties.joinToString("") { property ->
            val returnTypeString =
                typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
            val getter =
                "    private var ${property.name}Behavior: () -> $returnTypeString = " +
                        "{ super.${property.name} }\n"

            val setter =
                if (property.isMutable) {
                    "    private var set${property.name.capitalize()}Behavior: " +
                            "($returnTypeString) -> Unit = { value -> super.${property.name} = value }\n"
                } else {
                    ""
                }
            getter + setter
        }

    private fun buildMethodParameterTypes(parameters: List<ParameterAnalysis>): String =
        if (parameters.isEmpty()) {
            ""
        } else {
            parameters.joinToString(", ") { param ->
                if (param.isVararg) {
                    "Array<out ${unwrapVarargsType(param)}>"
                } else {
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                }
            }
        }

    private fun createErrorLambda(
        functionName: String,
        paramCount: Int,
    ): String =
        when (paramCount) {
            0 -> "{ error(\"Configure $functionName behavior\") }"
            1 -> "{ _ -> error(\"Configure $functionName behavior\") }"
            else -> {
                val params = List(paramCount) { "_" }.joinToString(", ")
                "{ $params -> error(\"Configure $functionName behavior\") }"
            }
        }

    private fun createSuperCallLambda(function: FunctionAnalysis): String {
        if (function.parameters.isEmpty()) {
            return "{ super.${function.name}() }"
        }

        val hasVarargs = function.parameters.any { it.isVararg }
        val varargsIndex = if (hasVarargs) function.parameters.indexOfFirst { it.isVararg } else -1
        val parameterNames =
            function.parameters
                .mapIndexed { index, param ->
                    val spread = if (param.isVararg) "*" else ""
                    val needsNamed = hasVarargs && index > varargsIndex
                    if (needsNamed) "${param.name} = ${param.name}" else "$spread${param.name}"
                }.joinToString(", ")

        val params = function.parameters.joinToString(", ") { it.name }
        return "{ $params -> super.${function.name}($parameterNames) }"
    }

    /**
     * Generates method overrides for class members.
     */
    private fun generateClassMethodOverrides(analysis: ClassAnalysis): String =
        buildString {
            append(generateMethodOverrides(analysis.abstractMethods))
            append(generateMethodOverrides(analysis.openMethods))
            append(generatePropertyOverrides(analysis.abstractProperties))
            append(generatePropertyOverrides(analysis.openProperties))
        }

    private fun generateMethodOverrides(methods: List<FunctionAnalysis>): String =
        methods.joinToString("") { function ->
            val returnTypeString = typeResolver.irTypeToKotlinString(
                function.returnType,
                preserveTypeParameters = true
            )
            val parameters = buildOverrideParameters(function.parameters)
            val parameterNames = function.parameters.joinToString(", ") { it.name }
            val suspendModifier = if (function.isSuspend) "suspend " else ""

            "    override ${suspendModifier}fun ${function.name}($parameters): $returnTypeString {\n" +
                    "        _${function.name}CallCount.update { it + 1 }\n" +
                    "        return ${function.name}Behavior($parameterNames)\n" +
                    "    }\n"
        }

    private fun generatePropertyOverrides(properties: List<PropertyAnalysis>): String =
        properties.joinToString("") { property ->
            val returnTypeString =
                typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)
            val varOrVal = if (property.isMutable) "var" else "val"

            if (property.isMutable) {
                "    override $varOrVal ${property.name}: $returnTypeString\n" +
                        "        get() {\n" +
                        "            _${property.name}CallCount.update { it + 1 }\n" +
                        "            return ${property.name}Behavior()\n" +
                        "        }\n" +
                        "        set(value) {\n" +
                        "            _set${property.name.capitalize()}CallCount.update { it + 1 }\n" +
                        "            set${property.name.capitalize()}Behavior(value)\n" +
                        "        }\n"
            } else {
                "    override $varOrVal ${property.name}: $returnTypeString get() {\n" +
                        "        _${property.name}CallCount.update { it + 1 }\n" +
                        "        return ${property.name}Behavior()\n" +
                        "    }\n"
            }
        }

    private fun buildOverrideParameters(parameters: List<ParameterAnalysis>): String =
        parameters.joinToString(", ") { param ->
            val varargsPrefix = if (param.isVararg) "vararg " else ""
            val paramType =
                if (param.isVararg) {
                    unwrapVarargsType(param)
                } else {
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                }
            "$varargsPrefix${param.name}: $paramType"
        }

    /**
     * Generates configuration methods for class members.
     */
    private fun generateClassConfigMethods(analysis: ClassAnalysis): String =
        buildString {
            (analysis.abstractMethods + analysis.openMethods).forEach { function ->
                append(buildSimpleClassConfigMethod(function))
            }

            (analysis.abstractProperties + analysis.openProperties).forEach { property ->
                append(generateClassPropertyConfigMethod(property))
            }
        }

    private fun buildSimpleClassConfigMethod(function: FunctionAnalysis): String {
        // Build context for method-level generic handling
        val parameterTypes = buildConfigParameterTypes(function)
        val returnTypeString =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
        val suspendModifier = if (function.isSuspend) "suspend " else ""

        return "    internal fun configure${function.name.capitalize()}(" +
                "behavior: $suspendModifier($parameterTypes) -> $returnTypeString" +
                ") { ${function.name}Behavior = behavior }\n"
    }

    private fun generateClassPropertyConfigMethod(property: PropertyAnalysis): String =
        buildString {
            val propertyName = property.name
            val returnTypeString =
                typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)

            appendLine(
                "    internal fun configure${propertyName.capitalize()}(" +
                        "behavior: () -> $returnTypeString" +
                        ") { ${propertyName}Behavior = behavior }",
            )

            if (property.isMutable) {
                appendLine(
                    "    internal fun configureSet${propertyName.capitalize()}(" +
                            "behavior: ($returnTypeString) -> Unit" +
                            ") { set${propertyName.capitalize()}Behavior = behavior }",
                )
            }
        }
}
