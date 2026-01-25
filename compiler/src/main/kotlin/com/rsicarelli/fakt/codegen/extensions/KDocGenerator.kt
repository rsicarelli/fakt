// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

/**
 * Memory-efficient KDoc generator for Fakt factory functions.
 *
 * Generates developer-friendly documentation that appears in IDE autocomplete,
 * helping developers understand how to configure fake implementations.
 *
 * Design principles:
 * - Uses Kotlin string templates with pre-calculated capacity for memory efficiency
 * - Runs at compile-time for potentially hundreds of interfaces
 * - Follows existing codebase patterns (buildString usage)
 *
 * Example output:
 * ```kotlin
 * /**
 *  * Creates a fake implementation of [UserService] for testing.
 *  *
 *  * Example:
 *  * ```kotlin
 *  * val fake = fakeUserService {
 *  *     getUser { userId -> User(id = userId, name = "Test") }
 *  *     saveUser { user -> Result.success(Unit) }
 *  * }
 *  * ```
 *  *
 *  * ## Configurable Behaviors
 *  * - `getUser`: (String) -> User?
 *  * - `saveUser`: (User) -> Result<Unit> (suspend)
 *  *
 *  * @param configure DSL block to configure fake behaviors
 *  * @return Configured [FakeUserServiceImpl] instance
 *  * @see UserService
 *  */
 * ```
 */
object KDocGenerator {
    /**
     * Base capacity estimate per item in the documentation.
     * Used for pre-sizing StringBuilder to avoid reallocations.
     */
    private const val BASE_CAPACITY = 256
    private const val PER_METHOD_CAPACITY = 80
    private const val PER_PROPERTY_CAPACITY = 60

    /**
     * Generates complete KDoc for a factory function.
     *
     * @param interfaceName The name of the interface being faked
     * @param factoryName The name of the factory function (e.g., "fakeUserService")
     * @param implClassName The name of the implementation class (e.g., "FakeUserServiceImpl")
     * @param methods List of method specifications for the interface
     * @param properties List of property specifications for the interface
     * @return Generated KDoc string (includes opening and closing comment markers)
     */
    fun generateFactoryKDoc(
        interfaceName: String,
        factoryName: String,
        implClassName: String,
        methods: List<MethodSpec> = emptyList(),
        properties: List<PropertySpec> = emptyList(),
    ): String {
        // Pre-calculate capacity to avoid reallocations
        val estimatedCapacity =
            BASE_CAPACITY +
                (methods.size * PER_METHOD_CAPACITY) +
                (properties.size * PER_PROPERTY_CAPACITY)

        return buildString(estimatedCapacity) {
            appendLine("/**")
            appendLine(" * Creates a fake implementation of [$interfaceName] for testing.")
            appendLine(" *")

            // Generate usage example if there are configurable behaviors
            if (methods.isNotEmpty() || properties.isNotEmpty()) {
                appendUsageExample(factoryName, methods, properties)
                appendLine(" *")
            }

            // Generate configurable behaviors section
            if (methods.isNotEmpty() || properties.isNotEmpty()) {
                appendConfigurableBehaviors(methods, properties)
                appendLine(" *")
            }

            // Standard @param and @return tags
            appendLine(" * @param configure DSL block to configure fake behaviors. Defaults to no-op.")
            appendLine(" * @return Configured [$implClassName] instance ready for testing")
            appendLine(" * @see $interfaceName")
            append(" */")
        }
    }

    /**
     * Appends usage example section to the StringBuilder.
     */
    private fun StringBuilder.appendUsageExample(
        factoryName: String,
        methods: List<MethodSpec>,
        properties: List<PropertySpec>,
    ) {
        appendLine(" * Example:")
        appendLine(" * ```kotlin")
        appendLine(" * val fake = $factoryName {")

        methods.forEach { method ->
            appendMethodExample(method)
        }

        properties.filter { !it.isStateFlow }.forEach { property ->
            appendPropertyExample(property)
        }

        appendLine(" * }")
        appendLine(" * ```")
    }

    /**
     * Appends a method configuration example.
     */
    private fun StringBuilder.appendMethodExample(method: MethodSpec) {
        val paramNames = method.params.map { (name, _, _) -> name }

        when {
            // No parameters: simple return example
            paramNames.isEmpty() -> {
                val exampleValue = getExampleReturnValue(method.returnType)
                appendLine(" *     ${method.name} { $exampleValue }")
            }

            // Single parameter: use 'it' for conciseness
            paramNames.size == 1 -> {
                val exampleValue = getExampleReturnValue(method.returnType, paramNames.first())
                appendLine(" *     ${method.name} { $exampleValue }")
            }

            // Multiple parameters: show named parameters
            else -> {
                val params = paramNames.joinToString(", ")
                val exampleValue = getExampleReturnValue(method.returnType)
                appendLine(" *     ${method.name} { $params -> $exampleValue }")
            }
        }
    }

    /**
     * Appends a property configuration example.
     */
    private fun StringBuilder.appendPropertyExample(property: PropertySpec) {
        val capitalizedName = property.name.replaceFirstChar { it.uppercase() }
        val exampleValue = getExampleReturnValue(property.type)

        if (property.isMutable) {
            appendLine(" *     configure$capitalizedName { $exampleValue }")
        } else {
            appendLine(" *     configure$capitalizedName { $exampleValue }")
        }
    }

    /**
     * Appends the configurable behaviors section.
     */
    private fun StringBuilder.appendConfigurableBehaviors(
        methods: List<MethodSpec>,
        properties: List<PropertySpec>,
    ) {
        appendLine(" * ## Configurable Behaviors")
        appendLine(" *")

        // Document methods
        methods.forEach { method ->
            appendMethodSignature(method)
        }

        // Document properties (non-StateFlow only, StateFlow has different API)
        properties.filter { !it.isStateFlow }.forEach { property ->
            appendPropertySignature(property)
        }
    }

    /**
     * Appends a method signature in documentation format.
     *
     * Format: `- \`methodName\`: (name: Type, ...) -> ReturnType [modifiers]`
     */
    private fun StringBuilder.appendMethodSignature(method: MethodSpec) {
        append(" * - `${method.name}`: ")

        // Build parameter list with names and types
        val paramList =
            if (method.extensionReceiverType != null) {
                val receiverParam = "receiver: ${method.extensionReceiverType}"
                val baseParams = method.params.map { (name, type, _) -> "$name: $type" }
                listOf(receiverParam) + baseParams
            } else {
                method.params.map { (name, type, _) -> "$name: $type" }
            }

        // Format as function type
        val paramsStr =
            if (paramList.isEmpty()) {
                "()"
            } else {
                "(${paramList.joinToString(", ")})"
            }

        append("$paramsStr -> ${method.returnType}")

        // Add modifiers
        val modifiers = mutableListOf<String>()
        if (method.isSuspend) modifiers.add("suspend")
        if (method.isOperator) modifiers.add("operator")
        if (method.extensionReceiverType != null) modifiers.add("extension")

        if (modifiers.isNotEmpty()) {
            append(" (${modifiers.joinToString(", ")})")
        }

        appendLine()
    }

    /**
     * Appends a property signature in documentation format.
     *
     * Format: `- \`propertyName\`: Type [mutable]`
     */
    private fun StringBuilder.appendPropertySignature(property: PropertySpec) {
        append(" * - `${property.name}`: ${property.type}")

        if (property.isMutable) {
            append(" (mutable)")
        }

        appendLine()
    }

    /** Exact type matches for example values. */
    private val exactTypeExamples =
        mapOf(
            "Unit" to "/* configure side effects */",
            "String" to "\"test\"",
            "Int" to "42",
            "Long" to "42L",
            "Double" to "3.14",
            "Float" to "3.14f",
            "Boolean" to "true",
        )

    /** Prefix-based type matches for example values. */
    private val prefixTypeExamples =
        mapOf(
            "List<" to "listOf(/* items */)",
            "Set<" to "setOf(/* items */)",
            "Map<" to "mapOf(/* entries */)",
            "Result<" to "Result.success(/* value */)",
            "Flow<" to "flowOf(/* values */)",
            "StateFlow<" to "MutableStateFlow(/* initial */)",
        )

    /**
     * Returns an example return value for documentation based on type.
     *
     * @param returnType The return type string
     * @param paramName Optional parameter name to use in the example
     * @return Example value string suitable for documentation
     */
    private fun getExampleReturnValue(
        returnType: String,
        paramName: String? = null,
    ): String =
        when {
            returnType == "String" && paramName != null -> "$paramName.uppercase()"
            exactTypeExamples.containsKey(returnType) -> exactTypeExamples.getValue(returnType)
            returnType.endsWith("?") -> paramName?.let { "get$it()" } ?: "null"
            else ->
                prefixTypeExamples.entries.firstOrNull { returnType.startsWith(it.key) }?.value
                    ?: paramName?.let { "process($it)" }
                    ?: "/* your implementation */"
        }
}

/**
 * Extension function to generate KDoc for a factory function configuration.
 *
 * Provides a convenient API for generating documentation alongside factory code.
 *
 * @param interfaceName The interface being faked
 * @param methods Methods from the interface
 * @param properties Properties from the interface
 * @return KDoc string for the factory function
 */
fun generateFactoryKDoc(
    interfaceName: String,
    methods: List<MethodSpec> = emptyList(),
    properties: List<PropertySpec> = emptyList(),
): String {
    val factoryName = "fake$interfaceName"
    val implClassName = "Fake${interfaceName}Impl"

    return KDocGenerator.generateFactoryKDoc(
        interfaceName = interfaceName,
        factoryName = factoryName,
        implClassName = implClassName,
        methods = methods,
        properties = properties,
    )
}
