// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Generator for call tracking functionality in IR phase.
 *
 * Creates call tracking infrastructure when @Fake(trackCalls = true):
 * - Call data classes to store method invocation data
 * - Call collection lists to store call history
 * - Call recording code in method overrides
 * - Verification method generation
 * - Cleanup methods for memory management
 *
 * Performance target: < 5% overhead when tracking is enabled
 * Memory management: Provides cleanup methods to prevent memory leaks
 */
class CallTrackingGenerator {

    /**
     * Determine if call tracking is needed based on annotation parameters.
     */
    fun needsCallTracking(trackCalls: Boolean): Boolean {
        return trackCalls
    }

    /**
     * Generate call data class name from method name.
     *
     * @param methodName The method name (e.g., "track")
     * @return Call data class name (e.g., "TrackCall")
     */
    fun generateCallDataClassName(methodName: String): String {
        return "${methodName.replaceFirstChar { it.titlecase() }}Call"
    }

    /**
     * Generate call collection code for a method.
     *
     * @param methodName The method name to track
     * @param parameters The method parameters to capture
     * @return Code to collect and store method call data
     */
    fun generateCallCollection(methodName: String, parameters: List<String>): String {
        val dataClassName = generateCallDataClassName(methodName)
        val paramNames = parameters.joinToString(", ")

        return """
        ${methodName}Calls.add($dataClassName($paramNames))
        """.trimIndent()
    }

    /**
     * Generate verification methods for a tracked method.
     *
     * @param methodName The method name to generate verification for
     * @return Code for verification methods
     */
    fun generateVerificationMethods(methodName: String): String {
        return """
        // Verification methods for $methodName
        internal fun verify${methodName.replaceFirstChar { it.titlecase() }}Tracked(times: Int = 1): Boolean =
            ${methodName}Calls.size == times

        internal fun verify${methodName.replaceFirstChar { it.titlecase() }}Never(): Boolean =
            ${methodName}Calls.isEmpty()
        """.trimIndent()
    }

    /**
     * Generate call tracking infrastructure for multiple methods.
     */
    fun generateMethodCallTracking(methods: List<String>): String {
        return methods.joinToString("\n\n") { method ->
            val dataClassName = generateCallDataClassName(method)
            """
            // Call tracking for $method
            data class $dataClassName(val param: Any) // TODO: Generate proper parameters
            internal val ${method}Calls = mutableListOf<$dataClassName>()
            """
        }
    }

    /**
     * Generate cleanup methods for call tracking.
     */
    fun generateCleanupMethods(methods: List<String>): String {
        val individualCleanup = methods.joinToString("\n    ") { method ->
            "fun clear${method.replaceFirstChar { it.titlecase() }}Calls() = ${method}Calls.clear()"
        }

        return """
        // Cleanup methods for call tracking
        $individualCleanup

        fun clearAllCalls() {
            ${methods.joinToString("\n        ") { "${it}Calls.clear()" }}
        }
        """.trimIndent()
    }
}
