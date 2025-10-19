// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.callTracking

import com.rsicarelli.fakt.Fake

/**
 * Interface designed specifically for testing edge cases of call tracking.
 *
 * Tests complex scenarios including:
 * - Recursive method calls
 * - High-frequency calls (performance/scalability)
 * - Suspend functions with cancellation
 * - Methods with default parameters
 * - Complex property access patterns
 * - Exception handling during calls
 */
@Fake
interface EdgeCaseService {
    /**
     * Method that can be used to test recursion.
     * Behavior should call itself recursively based on depth parameter.
     */
    fun recursiveMethod(depth: Int): Int

    /**
     * Method designed for high-frequency call testing.
     * Should be very lightweight to allow 10k+ calls.
     */
    fun highFrequencyMethod(value: Int): Int

    /**
     * Suspend method for testing cancellation scenarios.
     * Can simulate long-running operations.
     */
    suspend fun cancellableMethod(delayMs: Long): String

    /**
     * Method with default parameters to test overload tracking.
     */
    fun methodWithDefaults(
        required: String,
        optional1: Int = 42,
        optional2: Boolean = true,
    ): String

    /**
     * Method that throws exception to test tracking during errors.
     */
    fun throwingMethod(shouldThrow: Boolean): String

    /**
     * Property for testing complex getter scenarios.
     */
    val complexProperty: String

    /**
     * Mutable property for testing rapid set operations.
     */
    var rapidSetProperty: Int

    /**
     * Method for testing concurrent reads during writes.
     */
    suspend fun concurrentMethod(value: Int): Int

    /**
     * Method that simulates memory-intensive operations.
     * Used for GC/memory leak testing.
     */
    fun memoryIntensiveMethod(iterations: Int): List<String>
}
