// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.enums.generics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ValidationService fake with enums in generic contexts.
 *
 * Validates that enums work correctly within generic types like Result,
 * Pair, Triple, and as vararg parameters.
 */
class ValidationServiceTest {

    @Test
    fun `GIVEN ValidationService fake WHEN configuring validate THEN should return Result with enum status`() {
        // Given
        val validationService = fakeValidationService {
            validate { input ->
                when {
                    input.isBlank() -> Result.failure(IllegalArgumentException("Empty input"))
                    input.length < 3 -> Result.success(ValidationStatus.INVALID)
                    input.all { it.isLetterOrDigit() } -> Result.success(ValidationStatus.VALID)
                    else -> Result.success(ValidationStatus.INVALID)
                }
            }
        }

        // When
        val validResult = validationService.validate("abc123")
        val invalidResult = validationService.validate("ab")
        val failureResult = validationService.validate("")

        // Then
        assertTrue(validResult.isSuccess)
        assertEquals(ValidationStatus.VALID, validResult.getOrNull())

        assertTrue(invalidResult.isSuccess)
        assertEquals(ValidationStatus.INVALID, invalidResult.getOrNull())

        assertTrue(failureResult.isFailure)
    }

    @Test
    fun `GIVEN ValidationService fake WHEN configuring validateBatch THEN should return Result with nullable enum`() {
        // Given
        val validationService = fakeValidationService {
            validateBatch { inputs ->
                when {
                    inputs.isEmpty() -> Result.success(null)
                    inputs.all { it.isNotBlank() } -> Result.success(ValidationStatus.VALID)
                    else -> Result.success(ValidationStatus.INVALID)
                }
            }
        }

        // When
        val emptyResult = validationService.validateBatch(emptyList())
        val validResult = validationService.validateBatch(listOf("a", "b", "c"))
        val invalidResult = validationService.validateBatch(listOf("a", "", "c"))

        // Then
        assertTrue(emptyResult.isSuccess)
        assertNull(emptyResult.getOrNull())

        assertTrue(validResult.isSuccess)
        assertEquals(ValidationStatus.VALID, validResult.getOrNull())

        assertTrue(invalidResult.isSuccess)
        assertEquals(ValidationStatus.INVALID, invalidResult.getOrNull())
    }

    @Test
    fun `GIVEN ValidationService fake WHEN configuring hasAnyStatus THEN should check vararg enum parameters`() {
        // Given
        val validationService = fakeValidationService {
            hasAnyStatus { current, allowed ->
                allowed.contains(current)
            }
        }

        // When & Then
        assertTrue(
            validationService.hasAnyStatus(
                ValidationStatus.VALID,
                ValidationStatus.VALID,
                ValidationStatus.PENDING
            )
        )
        assertTrue(
            validationService.hasAnyStatus(
                ValidationStatus.INVALID,
                ValidationStatus.INVALID
            )
        )
        assertFalse(
            validationService.hasAnyStatus(
                ValidationStatus.EXPIRED,
                ValidationStatus.VALID,
                ValidationStatus.PENDING
            )
        )
    }

    @Test
    fun `GIVEN ValidationService fake WHEN configuring getStatusWithTimestamp THEN should return Pair with enum`() {
        // Given
        val validationService = fakeValidationService {
            getStatusWithTimestamp { input ->
                val status = if (input.isNotBlank()) ValidationStatus.VALID else ValidationStatus.INVALID
                val timestamp = System.currentTimeMillis()
                status to timestamp
            }
        }

        // When
        val (status, timestamp) = validationService.getStatusWithTimestamp("test")

        // Then
        assertEquals(ValidationStatus.VALID, status)
        assertTrue(timestamp > 0)
    }

    @Test
    fun `GIVEN ValidationService fake WHEN configuring getValidationDetails THEN should return Triple with enum`() {
        // Given
        val validationService = fakeValidationService {
            getValidationDetails { input ->
                when {
                    input.isBlank() -> Triple(ValidationStatus.INVALID, "Input is blank", 400)
                    input.length < 3 -> Triple(ValidationStatus.INVALID, "Too short", 400)
                    else -> Triple(ValidationStatus.VALID, "OK", 200)
                }
            }
        }

        // When
        val (status1, message1, code1) = validationService.getValidationDetails("")
        val (status2, message2, code2) = validationService.getValidationDetails("ab")
        val (status3, message3, code3) = validationService.getValidationDetails("abc")

        // Then
        assertEquals(ValidationStatus.INVALID, status1)
        assertEquals("Input is blank", message1)
        assertEquals(400, code1)

        assertEquals(ValidationStatus.INVALID, status2)
        assertEquals("Too short", message2)
        assertEquals(400, code2)

        assertEquals(ValidationStatus.VALID, status3)
        assertEquals("OK", message3)
        assertEquals(200, code3)
    }

    @Test
    fun `GIVEN ValidationService fake WHEN configuring combineStatuses THEN should combine vararg enums into Result`() {
        // Given
        val validationService = fakeValidationService {
            combineStatuses { statuses ->
                when {
                    statuses.isEmpty() -> Result.failure(IllegalArgumentException("No statuses"))
                    statuses.all { it == ValidationStatus.VALID } -> Result.success(ValidationStatus.VALID)
                    statuses.any { it == ValidationStatus.INVALID } -> Result.success(ValidationStatus.INVALID)
                    else -> Result.success(ValidationStatus.PENDING)
                }
            }
        }

        // When
        val allValid = validationService.combineStatuses(
            ValidationStatus.VALID,
            ValidationStatus.VALID,
            ValidationStatus.VALID
        )
        val hasInvalid = validationService.combineStatuses(
            ValidationStatus.VALID,
            ValidationStatus.INVALID,
            ValidationStatus.VALID
        )
        val pending = validationService.combineStatuses(
            ValidationStatus.PENDING,
            ValidationStatus.PENDING
        )

        // Then
        assertTrue(allValid.isSuccess)
        assertEquals(ValidationStatus.VALID, allValid.getOrNull())

        assertTrue(hasInvalid.isSuccess)
        assertEquals(ValidationStatus.INVALID, hasInvalid.getOrNull())

        assertTrue(pending.isSuccess)
        assertEquals(ValidationStatus.PENDING, pending.getOrNull())
    }

    @Test
    fun `GIVEN ValidationService fake WHEN configuring getOptionalStatus THEN should return nullable enum`() {
        // Given
        val validationService = fakeValidationService {
            getOptionalStatus { input ->
                when {
                    input.isBlank() -> null
                    input.length < 3 -> ValidationStatus.INVALID
                    else -> ValidationStatus.VALID
                }
            }
        }

        // When & Then
        assertNull(validationService.getOptionalStatus(""))
        assertEquals(ValidationStatus.INVALID, validationService.getOptionalStatus("ab"))
        assertEquals(ValidationStatus.VALID, validationService.getOptionalStatus("abc"))
    }
}
