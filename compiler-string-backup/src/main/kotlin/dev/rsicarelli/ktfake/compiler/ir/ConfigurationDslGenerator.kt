// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Generator for configuration DSL classes in IR phase.
 *
 * Creates configuration DSL that provides a clean API for behavior setup:
 * ```kotlin
 * val userService = fakeUserService {
 *     getUser { id -> User(id, "Test User") }
 *     // OR with default return
 *     getUser(returns = testUser)
 *     // OR with exception
 *     getUser(throws = RuntimeException("Error"))
 * }
 * ```
 *
 * Configuration DSL classes delegate to the implementation's configure methods.
 * This provides a user-friendly API while maintaining the underlying structure.
 */
class ConfigurationDslGenerator {

    /**
     * Generate a configuration DSL class name from an interface name.
     *
     * @param interfaceName The original interface name (e.g., "UserService")
     * @return The configuration class name (e.g., "FakeUserServiceConfig")
     */
    fun generateConfigName(interfaceName: String): String {
        return "Fake${interfaceName}Config"
    }

    /**
     * Generate a configuration DSL class for a given interface.
     *
     * TODO: Implement actual IR generation when IR API integration is ready
     */
    fun generateConfigDsl(interfaceName: String, methodSignatures: List<String>): String {
        val configName = generateConfigName(interfaceName)
        val implName = "Fake${interfaceName}Impl"

        val configMethods = methodSignatures.joinToString("\n") { signature ->
            when {
                // Handle property signatures: "val memes: String" -> "memes"
                signature.startsWith("val ") -> {
                    val propertyName = signature.removePrefix("val ").substringBefore(":")
                    val propertyType = signature.substringAfter(": ").trim()
                    generateConfigMethod(propertyName, propertyType)
                }
                // Handle method signatures with suspend support
                signature.contains("(") -> {
                    // Handle suspend functions in DSL
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
                    generateConfigMethod(methodName, returnType, isSuspend)
                }
                // Fallback for malformed signatures
                else -> {
                    generateConfigMethod("unknownMember")
                }
            }
        }

        return """
class $configName(private val fake: $implName) {
$configMethods
}
        """.trimIndent()
    }

    /**
     * Generate configuration methods that delegate to implementation configuration.
     */
    private fun generateConfigMethod(methodName: String, returnType: String = "Any", isSuspend: Boolean = false): String {
        val configMethodName = "configure${methodName.replaceFirstChar { it.titlecase() }}"
        val behaviorType = if (isSuspend) {
            "suspend () -> $returnType"
        } else {
            "() -> $returnType"
        }

        return """
    // Configuration for $methodName
    fun $methodName(behavior: $behaviorType) {
        fake.$configMethodName(behavior)
    }
        """.trimIndent()
    }

    /**
     * Generate configuration function name for a method.
     */
    fun generateConfigFunctionName(methodName: String): String {
        return methodName
    }

    /**
     * Support for suspend methods in configuration DSL.
     */
    fun generateSuspendConfigMethod(methodName: String): String {
        return """
        // Suspend behavior configuration
        fun $methodName(behavior: suspend () -> Any) {
            fake.configure${methodName.replaceFirstChar { it.titlecase() }} {
                runBlocking { behavior() }
            }
        }
        """.trimIndent()
    }
}
