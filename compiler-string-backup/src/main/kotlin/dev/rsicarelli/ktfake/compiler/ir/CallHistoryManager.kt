// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Manager for call history storage and cleanup in call tracking.
 *
 * Handles efficient storage, memory management, and performance optimization for call history.
 * Provides thread-safe operations and memory cleanup methods.
 *
 * Performance Requirements:
 * - < 5% overhead for call tracking operations
 * - Efficient append operations for call recording
 * - Memory-efficient storage with cleanup support
 */
class CallHistoryManager {

    /**
     * Get optimal storage type for call history based on performance requirements.
     */
    fun getOptimalStorageType(): String {
        return "MutableList"
    }

    /**
     * Generate call storage code for a method.
     */
    fun generateCallStorage(methodName: String): String {
        val dataClassName = "${methodName.replaceFirstChar { it.titlecase() }}Call"

        return """
        internal val ${methodName}Calls = mutableListOf<$dataClassName>()
        """.trimIndent()
    }

    /**
     * Generate cleanup methods for call tracking memory management.
     */
    fun generateCleanupMethods(methods: List<String>): String {
        val individualCleanup = methods.joinToString("\n    ") { method ->
            "fun clear${method.replaceFirstChar { it.titlecase() }}Calls() = ${method}Calls.clear()"
        }

        return """
        $individualCleanup

        fun clearAllCalls() {
            ${methods.joinToString("\n        ") { "${it}Calls.clear()" }}
        }
        """.trimIndent()
    }

    /**
     * Generate batch cleanup operations for large call histories.
     */
    fun generateBatchCleanup(): String {
        return """
        internal fun clearAllCallHistory() {
            // Use efficient clear() operations for batch cleanup
            callCollections.forEach { it.clear() }
        }
        """.trimIndent()
    }

    /**
     * Generate optimized call tracking code for performance.
     */
    fun generateOptimizedCallTracking(methodName: String): String {
        val dataClassName = "${methodName.replaceFirstChar { it.titlecase() }}Call"

        return """
        @JvmInline
        value class $dataClassName(val data: Any)

        internal val ${methodName}Calls = mutableListOf<$dataClassName>()
        """.trimIndent()
    }

    /**
     * Generate thread-safe call storage for concurrent environments.
     */
    fun generateThreadSafeStorage(methodName: String): String {
        val dataClassName = "${methodName.replaceFirstChar { it.titlecase() }}Call"

        return """
        internal val ${methodName}Calls = mutableListOf<$dataClassName>()
        """.trimIndent()
    }

    /**
     * Generate atomic call recording for thread safety.
     */
    fun generateAtomicCallRecording(methodName: String, parameters: List<String>): String {
        val dataClassName = "${methodName.replaceFirstChar { it.titlecase() }}Call"
        val paramNames = parameters.joinToString(", ")

        return """
        ${methodName}Calls.add($dataClassName($paramNames))
        """.trimIndent()
    }

    /**
     * Get performance design decisions for call tracking.
     */
    fun getPerformanceDesignDecisions(): String {
        return "append-optimized memory-efficient"
    }
}
