// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.generics

import com.rsicarelli.fakt.samples.singleModule.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Simple DTO for transformation tests
 */
data class UserDto(val id: String, val displayName: String)

/**
 * Tests for P1 Scenario: GenericTransformationClass
 *
 * TESTING STANDARD: GIVEN-WHEN-THEN pattern (uppercase)
 * Framework: Vanilla JUnit5 + kotlin-test
 *
 * Validates generic transformation class fakes with In/Out type parameters.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataTransformerTest {

    @Test
    fun `GIVEN transformer String to Int WHEN transforming THEN should use custom logic`() {
        // Given
        val transformer: DataTransformer<String, Int> = fakeDataTransformer {
            transform { input -> input.toIntOrNull() ?: 0 }
        }

        // When
        val result1 = transformer.transform("42")
        val result2 = transformer.transform("invalid")

        // Then
        assertEquals(42, result1)
        assertEquals(0, result2) // Fallback for invalid input
    }

    @Test
    fun `GIVEN transformer User to UserDto WHEN batch transforming THEN should map all items`() {
        // Given
        val transformer: DataTransformer<User, UserDto> = fakeDataTransformer {
            transform { user -> UserDto(user.id, user.name) }
            transformBatch { users -> users.map { UserDto(it.id, it.name.uppercase()) } }
        }

        val users = listOf(
            User("1", "Alice"),
            User("2", "Bob"),
        )

        // When
        val dtos = transformer.transformBatch(users)

        // Then - Custom batch logic (uppercase names)
        assertEquals(2, dtos.size)
        assertEquals(UserDto("1", "ALICE"), dtos[0])
        assertEquals(UserDto("2", "BOB"), dtos[1])
    }

    @Test
    fun `GIVEN configured canTransform WHEN checking THEN should use predicate`() {
        // Given
        val transformer: DataTransformer<String, Int> = fakeDataTransformer {
            transform { input -> input.length }
            canTransform { input -> input.isNotBlank() }
        }

        // When
        val canTransformValid = transformer.canTransform("hello")
        val canTransformInvalid = transformer.canTransform("")

        // Then
        assertTrue(canTransformValid)
        assertFalse(canTransformInvalid)
    }

    @Test
    fun `GIVEN unconfigured transformer WHEN transforming THEN should error from super implementation`() {
        // Given
        val transformer: DataTransformer<String, Int> = fakeDataTransformer {}

        // When/Then - Transform method has error() in super
        assertFailsWith<IllegalStateException> {
            transformer.transform("test")
        }
    }

    @Test
    fun `GIVEN transformer with same In Out types WHEN using THEN should work with identity pattern`() {
        // Given - Both type parameters are same type
        val transformer: DataTransformer<String, String> = fakeDataTransformer {
            transform { input -> input.uppercase() }
            canTransform { input -> input.length > 3 }
        }

        // When
        val result = transformer.transform("hello")
        val canTransform = transformer.canTransform("hi")

        // Then - Works correctly with same type for In and Out
        assertEquals("HELLO", result)
        assertFalse(canTransform) // "hi" length is 2, not > 3
    }
}
