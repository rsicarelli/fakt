// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.defaultParameters

import com.rsicarelli.fakt.Fake

/**
 * Test interface demonstrating comprehensive default parameter support.
 *
 * This interface covers all common default parameter scenarios to ensure
 * the Fakt compiler correctly preserves default values in generated fakes.
 *
 * ## Scenarios Covered:
 * 1. Single default parameter
 * 2. All parameters with defaults
 * 3. Mixed parameters (some with, some without defaults)
 * 4. Nullable default values
 * 5. Suspend functions with defaults
 * 6. Complex type defaults (collections)
 */
@Fake
interface DefaultParametersService {
    /**
     * Scenario 1: Single default parameter
     * Tests that a single trailing default parameter is preserved.
     */
    fun singleDefault(
        amount: Double,
        currency: String = "USD",
    ): String

    /**
     * Scenario 2: All parameters have defaults
     * Tests that all defaults are preserved when every parameter has one.
     */
    fun allDefaults(
        timeout: Long = 5000L,
        retries: Int = 3,
        enabled: Boolean = true,
    ): String

    /**
     * Scenario 3: Mixed parameters (required + optional)
     * Tests that defaults work correctly when mixed with required parameters.
     */
    fun mixedDefaults(
        required: String,
        optional: Int = 42,
        another: String = "default",
    ): String

    /**
     * Scenario 4: Nullable default value
     * Tests that nullable types with null defaults are preserved.
     */
    fun nullableDefault(
        id: String,
        metadata: Map<String, String>? = null,
    ): Result<String>

    /**
     * Scenario 5: Suspend function with defaults
     * Tests that suspend functions preserve default parameters.
     */
    suspend fun suspendWithDefaults(
        url: String,
        timeout: Long = 3000L,
    ): Result<String>

    /**
     * Scenario 6: Complex type defaults (collections)
     * Tests that collection defaults like emptyList() and emptyMap() are preserved.
     */
    fun complexDefaults(
        items: List<String> = emptyList(),
        config: Map<String, Any> = emptyMap(),
    ): Int

    /**
     * Scenario 7: Multiple defaults with different types
     * Tests various primitive type defaults together.
     */
    fun primitiveDefaults(
        name: String = "",
        count: Int = 0,
        rate: Double = 0.0,
        active: Boolean = false,
    ): String

    /**
     * Scenario 8: Default in middle position
     * Tests that defaults work correctly when not at the end of parameter list.
     */
    fun defaultInMiddle(
        first: String,
        middle: Int = 10,
        last: String,
    ): String
}
