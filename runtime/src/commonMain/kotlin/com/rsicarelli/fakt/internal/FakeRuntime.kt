// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.internal

import kotlin.time.TimeSource

/**
 * Internal runtime support for generated fakes.
 *
 * This class provides common functionality used by all generated fake implementations.
 * It handles scope management, call tracking infrastructure, and configuration DSL support.
 *
 * This is an internal API and should not be used directly in user code.
 */
@PublishedApi
internal object FakeRuntime {

    /**
     * Current fake scope for instance management.
     * Determines how long fake instances remain valid.
     */
    @PublishedApi
    internal var currentScope: String = "test"

    /**
     * Registry of active fake instances by scope and type.
     * Used for scope-based instance management.
     */
    @PublishedApi
    internal val instanceRegistry: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()

    /**
     * Thread-local call tracking storage.
     * Stores method call information when trackCalls is enabled.
     */
    @PublishedApi
    internal val callTracker: MutableMap<String, MutableList<CallRecord>> = mutableMapOf()

    /**
     * Sets the current fake scope.
     * This affects how fake instances are managed and shared.
     */
    fun setScope(scope: String) {
        currentScope = scope
    }

    /**
     * Clears all fake instances for a given scope.
     * Called automatically at scope boundaries (e.g., test method end).
     */
    fun clearScope(scope: String) {
        instanceRegistry.remove(scope)
        callTracker.clear() // Clear call tracking data with scope
    }

    /**
     * Gets or creates a fake instance for the current scope.
     * Implements the singleton-per-scope pattern for non-concurrent fakes.
     */
    inline fun <T> getInstance(
        type: String,
        concurrent: Boolean,
        factory: () -> T
    ): T {
        return if (concurrent) {
            // Always create new instance for thread safety
            factory()
        } else {
            // Reuse instance within scope
            val scopeRegistry = instanceRegistry.getOrPut(currentScope) { mutableMapOf() }
            @Suppress("UNCHECKED_CAST")
            scopeRegistry.getOrPut(type) { factory() as Any } as T
        }
    }

    /**
     * Records a method call for tracking purposes.
     * Used when trackCalls is enabled on the fake.
     */
    fun recordCall(
        instanceId: String,
        methodName: String,
        arguments: Array<Any?>,
        returnValue: Any?,
        timestamp: Long = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    ) {
        val key = "$instanceId.$methodName"
        val calls = callTracker.getOrPut(key) { mutableListOf() }
        calls.add(CallRecord(methodName, arguments.toList(), returnValue, timestamp))
    }
}

/**
 * Data class representing a recorded method call.
 * Used for call tracking and verification functionality.
 */
@PublishedApi
internal data class CallRecord(
    val methodName: String,
    val arguments: List<Any?>,
    val returnValue: Any?,
    val timestamp: Long
)
