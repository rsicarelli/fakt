// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.enums.generics

import com.rsicarelli.fakt.Fake

/**
 * Validation service using enums in generic contexts.
 *
 * Tests:
 * - Result<Enum> generic type with enum
 * - Result<Enum?> nullable generic enum
 * - vararg parameters with enums
 * - Pair/Triple with enums
 */
@Fake
interface ValidationService {
    /**
     * Validate input and return result with enum status.
     */
    fun validate(input: String): Result<ValidationStatus>

    /**
     * Validate multiple inputs and return nullable enum result.
     */
    fun validateBatch(inputs: List<String>): Result<ValidationStatus?>

    /**
     * Check if any of the provided statuses match.
     * Tests vararg with enums.
     */
    fun hasAnyStatus(
        current: ValidationStatus,
        vararg allowed: ValidationStatus,
    ): Boolean

    /**
     * Get validation status with timestamp.
     * Tests Pair with enum.
     */
    fun getStatusWithTimestamp(input: String): Pair<ValidationStatus, Long>

    /**
     * Get validation details (status, message, code).
     * Tests Triple with enum.
     */
    fun getValidationDetails(input: String): Triple<ValidationStatus, String, Int>

    /**
     * Combine multiple statuses into final result.
     * Tests vararg with enum and Result.
     */
    fun combineStatuses(vararg statuses: ValidationStatus): Result<ValidationStatus>

    /**
     * Get nullable validation status.
     * Tests generic with nullable enum.
     */
    fun getOptionalStatus(input: String): ValidationStatus?
}
