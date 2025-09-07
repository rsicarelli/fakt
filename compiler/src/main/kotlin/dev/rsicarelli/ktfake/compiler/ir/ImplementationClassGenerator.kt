// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Generator for implementation classes in IR phase.
 *
 * Creates implementation classes that provide concrete implementations for interfaces
 * marked with @Fake annotation. Handles method overrides and class structure.
 */
class ImplementationClassGenerator {

    /**
     * Generate basic implementation class structure.
     */
    fun generateImplementationClass(interfaceName: String): String {
        return """
        class Fake${interfaceName}Impl : $interfaceName {
            // Implementation methods will be added based on interface analysis
        }
        """.trimIndent()
    }

    /**
     * Generate method and property overrides with configurable behavior support.
     */
    fun generateMethodOverrides(signatures: List<String>): String {
        val behaviorFields = mutableListOf<String>()
        val methodOverrides = mutableListOf<String>()

        signatures.forEach { signature ->
            when {
                // Handle property signatures (val propertyName: Type)
                signature.startsWith("val ") -> {
                    val propertyName = signature.removePrefix("val ").substringBefore(":")
                    val propertyType = signature.substringAfter(": ").trim()

                    // Add behavior field
                    behaviorFields.add("    private var ${propertyName}Behavior: () -> $propertyType = { ${getDefaultValue(propertyType)} }")

                    // Add configurable property override
                    methodOverrides.add("    override val $propertyName: $propertyType get() = ${propertyName}Behavior()")
                }
                // Handle method signatures with suspend support
                signature.contains("(") -> {
                    // Check if method is suspend
                    val isSuspend = signature.startsWith("suspend ")
                    val cleanSignature = if (isSuspend) signature.removePrefix("suspend ") else signature
                    val methodName = cleanSignature.substringBefore("(")

                    // Parse return type correctly - it's after the closing parenthesis and colon
                    val returnType = if (cleanSignature.contains("): ")) {
                        cleanSignature.substringAfter("): ").trim()
                    } else if (cleanSignature.contains(")")) {
                        "Unit" // No return type specified means Unit
                    } else {
                        "Unit"
                    }

                    // Add behavior field - suspend functions need suspend lambda types
                    val behaviorType = if (isSuspend) {
                        if (returnType == "Unit") "suspend () -> Unit" else "suspend () -> $returnType"
                    } else {
                        if (returnType == "Unit") "() -> Unit" else "() -> $returnType"
                    }
                    behaviorFields.add("    private var ${methodName}Behavior: $behaviorType = { ${getDefaultValue(returnType)} }")

                    // Add configurable method override with proper suspend modifier placement
                    val methodBody = if (returnType == "Unit") {
                        " { ${methodName}Behavior() }"
                    } else {
                        " = ${methodName}Behavior()"
                    }
                    
                    // For suspend functions, build the override with suspend modifier before fun
                    val overrideSignature = if (isSuspend) {
                        "    override suspend fun $cleanSignature$methodBody"
                    } else {
                        "    override fun $signature$methodBody"
                    }
                    methodOverrides.add(overrideSignature)
                }
            }
        }

        return (behaviorFields + listOf("") + methodOverrides).joinToString("\n")
    }

    /**
     * Get default value for a type.
     */
    private fun getDefaultValue(type: String): String {
        return when (type.trim()) {
            "String" -> "\"\""
            "Int" -> "0"
            "Boolean" -> "false"
            "Long" -> "0L"
            "Double" -> "0.0"
            "Float" -> "0.0f"
            "Unit" -> ""
            else -> "null"
        }
    }

    /**
     * Generate complete implementation with method overrides and configuration support.
     */
    fun generateCompleteImplementation(interfaceName: String, methods: List<String>): String {
        val methodOverrides = generateMethodOverrides(methods)
        val configurationMethods = generateConfigurationMethods(methods)

        return """
class Fake${interfaceName}Impl : $interfaceName {
$methodOverrides

    // Configuration methods for behavior setup
$configurationMethods
}
        """.trimIndent()
    }

    /**
     * Generate internal configuration methods that the DSL can call.
     */
    private fun generateConfigurationMethods(signatures: List<String>): String {
        return signatures.mapNotNull { signature ->
            when {
                signature.startsWith("val ") -> {
                    val propertyName = signature.removePrefix("val ").substringBefore(":")
                    val propertyType = signature.substringAfter(": ").trim()
                    "    internal fun configure${propertyName.replaceFirstChar { it.titlecase() }}(behavior: () -> $propertyType) { ${propertyName}Behavior = behavior }"
                }
                signature.contains("(") -> {
                    // Handle suspend functions in configuration
                    val isSuspend = signature.startsWith("suspend ")
                    val cleanSignature = if (isSuspend) signature.removePrefix("suspend ") else signature
                    val methodName = cleanSignature.substringBefore("(")

                    // Parse return type correctly - it's after the closing parenthesis and colon
                    val returnType = if (cleanSignature.contains("): ")) {
                        cleanSignature.substringAfter("): ").trim()
                    } else if (cleanSignature.contains(")")) {
                        "Unit" // No return type specified means Unit
                    } else {
                        "Unit"
                    }

                    val behaviorType = if (isSuspend) {
                        if (returnType == "Unit") "suspend () -> Unit" else "suspend () -> $returnType"
                    } else {
                        if (returnType == "Unit") "() -> Unit" else "() -> $returnType"
                    }
                    "    internal fun configure${methodName.replaceFirstChar { it.titlecase() }}(behavior: $behaviorType) { ${methodName}Behavior = behavior }"
                }
                else -> null
            }
        }.joinToString("\n")
    }
}
