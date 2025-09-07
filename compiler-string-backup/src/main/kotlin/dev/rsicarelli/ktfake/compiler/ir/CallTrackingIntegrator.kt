// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Integrator for call tracking with existing implementation generation.
 *
 * Coordinates between implementation class generation and call tracking functionality.
 * Ensures seamless integration without breaking existing API patterns.
 */
class CallTrackingIntegrator(
    private val implementationGenerator: ImplementationClassGenerator,
    private val callTrackingGenerator: CallTrackingGenerator
) {

    /**
     * Generate implementation class with optional call tracking.
     */
    fun generateImplementation(
        interfaceName: String,
        methods: List<String>,
        trackCalls: Boolean
    ): String {
        if (!trackCalls) {
            return implementationGenerator.generateCompleteImplementation(interfaceName, methods)
        }

        val callDataClasses = methods.joinToString("\n\n") { method ->
            val dataClassName = callTrackingGenerator.generateCallDataClassName(method)
            "data class $dataClassName(val param: Any)"
        }

        val callStorageLists = methods.joinToString("\n    ") { method ->
            val dataClassName = callTrackingGenerator.generateCallDataClassName(method)
            "internal val ${method}Calls = mutableListOf<$dataClassName>()"
        }

        val verificationMethods = methods.joinToString("\n\n") { method ->
            callTrackingGenerator.generateVerificationMethods(method)
        }

        return """
        $callDataClasses

        class Fake${interfaceName}Impl : $interfaceName {
            $callStorageLists

            ${methods.joinToString("\n    ") { method -> "override fun $method() { ${method}Calls.add(${callTrackingGenerator.generateCallDataClassName(method)}(Unit)) }" }}

            $verificationMethods
        }
        """.trimIndent()
    }

    /**
     * Generate factory function with call tracking support.
     */
    fun generateFactoryWithTracking(interfaceName: String, trackCalls: Boolean): String {
        return """
        fun fake$interfaceName(configure: Fake${interfaceName}Config.() -> Unit = {}): $interfaceName {
            return Fake${interfaceName}Impl().apply { Fake${interfaceName}Config(this).configure() }
        }
        """.trimIndent()
    }

    /**
     * Generate configuration DSL with call tracking access.
     */
    fun generateConfigurationDslWithTracking(
        interfaceName: String,
        methods: List<String>,
        trackCalls: Boolean
    ): String {
        if (!trackCalls) {
            return "class Fake${interfaceName}Config(private val impl: Fake${interfaceName}Impl)"
        }

        return """
        class Fake${interfaceName}Config(private val impl: Fake${interfaceName}Impl) {
            fun getVerification(): Fake${interfaceName}Verification = Fake${interfaceName}Verification(impl)
            fun clearCalls() = impl.clearAllCalls()
        }
        """.trimIndent()
    }

    /**
     * Generate optimized implementation for performance-critical scenarios.
     */
    fun generateOptimizedImplementation(
        interfaceName: String,
        methods: List<String>,
        trackCalls: Boolean
    ): String {
        if (!trackCalls) {
            return """
            class Fake${interfaceName}Impl : $interfaceName {
                ${methods.joinToString("\n    ") { "override fun $it() {}" }}
            }
            """.trimIndent()
        }

        return generateImplementation(interfaceName, methods, trackCalls)
    }
}
