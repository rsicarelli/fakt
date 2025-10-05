// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Comprehensive test for GenericEventProcessor fake generation.
 *
 * Validates:
 * - Class-level generic type parameters
 * - Method-level generic type parameters
 * - Lambda parameters with generics
 * - Type safety across transformations
 */
class GenericEventProcessorTest {
    @Test
    fun `GIVEN GenericEventProcessor WHEN processing item THEN should preserve class-level type T`() {
        // Given
        val fake =
            fakeGenericEventProcessor<String> {
                process { item, processor ->
                    processor(item.uppercase())
                }
            }

        // When
        val result = fake.process("hello") { "Processed: $it" }

        // Then
        assertEquals("Processed: HELLO", result)
    }

    @Test
    fun `GIVEN GenericEventProcessor WHEN transforming THEN should preserve method-level type R`() {
        // Given
        val fake =
            fakeGenericEventProcessor<Int> {
                transform<Any?> { items, transformer ->
                    items.map(transformer)
                }
            }

        // When
        val result = fake.transform(listOf(1, 2, 3)) { it * 2 }

        // Then
        assertEquals(listOf(2, 4, 6), result)
    }

    @Test
    fun `GIVEN GenericEventProcessor WHEN configured with User type THEN should maintain type safety`() {
        // Given
        data class User(
            val id: String,
            val name: String,
        )
        val fake =
            fakeGenericEventProcessor<User> {
                process { user, processor ->
                    processor(user.copy(name = user.name.uppercase()))
                }
                transform<Any?> { users, transformer ->
                    users.map(transformer)
                }
            }

        // When
        val processResult = fake.process(User("1", "alice")) { "User: ${it.name}" }
        val transformResult = fake.transform(listOf(User("1", "Bob"))) { it.name }

        // Then
        assertEquals("User: ALICE", processResult)
        assertEquals(listOf("Bob"), transformResult)
    }

    @Test
    fun `GIVEN GenericEventProcessor WHEN using defaults THEN should handle Any defaults`() {
        // Given
        val fake = fakeGenericEventProcessor<String>()

        // When
        val processResult = fake.process("test") { "result" }
        val transformResult = fake.transform(listOf("a", "b")) { it.length }

        // Then
        assertEquals("", processResult) // Default: empty string
        assertEquals(0, transformResult.size) // Default: empty list
    }
}
