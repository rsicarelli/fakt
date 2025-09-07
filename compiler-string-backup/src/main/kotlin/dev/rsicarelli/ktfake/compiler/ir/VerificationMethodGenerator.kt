// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Generator for verification methods in call tracking.
 *
 * Creates comprehensive verification API for tracked method calls:
 * - verifyTracked: Exact call count verification
 * - verifyNever: Verify method was never called
 * - verifyAtLeast: Minimum call count verification
 * - verifyAtMost: Maximum call count verification
 * - getCalls: Access to raw call data for inspection
 * - Parameter matching: Verify calls with specific parameters
 */
class VerificationMethodGenerator {

    /**
     * Generate verifyTracked method for exact call count verification.
     */
    fun generateVerifyTracked(methodName: String): String {
        val dataClassName = "${methodName.replaceFirstChar { it.titlecase() }}Call"

        return """
        internal fun verifyTracked(times: Int = 1): Boolean =
            ${methodName}Calls.count { it.event == event } == times
        """.trimIndent()
    }

    /**
     * Generate verifyNever method for never-called verification.
     */
    fun generateVerifyNever(methodName: String): String {
        return """
        internal fun verifyNever(): Boolean =
            ${methodName}Calls.none { true }
        """.trimIndent()
    }

    /**
     * Generate verifyAtLeast method for minimum call count verification.
     */
    fun generateVerifyAtLeast(methodName: String): String {
        return """
        internal fun verifyAtLeast(times: Int): Boolean =
            ${methodName}Calls.count { it.event == event } >= times
        """.trimIndent()
    }

    /**
     * Generate verifyAtMost method for maximum call count verification.
     */
    fun generateVerifyAtMost(methodName: String): String {
        return """
        internal fun verifyAtMost(times: Int): Boolean =
            ${methodName}Calls.count { it.event == event } <= times
        """.trimIndent()
    }

    /**
     * Generate getCalls method for raw call data access.
     */
    fun generateGetCalls(methodName: String): String {
        val dataClassName = "${methodName.replaceFirstChar { it.titlecase() }}Call"

        return """
        internal fun getTrackCalls(): List<$dataClassName> =
            ${methodName}Calls.toList()
        """.trimIndent()
    }

    /**
     * Generate parameter matching verification methods.
     */
    fun generateParameterMatchingVerification(methodName: String, parameters: List<String>): String {
        val parameterChecks = parameters.joinToString(",\n        ") { param ->
            val paramName = param.split(":")[0].trim()
            "$paramName: ${param.split(":")[1].trim()}? = null"
        }

        return """
        internal fun verifyTracked(
            $parameterChecks,
            times: Int = 1
        ): Boolean {
            return ${methodName}Calls.count { call ->
                true
            } == times
        }
        """.trimIndent()
    }
}
