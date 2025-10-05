// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces
import kotlinx.coroutines.test.runTest

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.AsyncTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeAsyncTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.ComparableProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeComparableProcessor
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.Converter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeConverter
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.ListMapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeListMapper
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.MultiConstraintHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeMultiConstraintHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.NullableTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeNullableTransformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.ResultHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeResultHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.Transformer
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeTransformer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Phase 2: SAM with Class-Level Generics Tests (P0 - Must Work)
 *
 * Tests SAM interfaces with class-level type parameters:
 * - Single type parameter
 * - Multiple type parameters
 * - Generic constraints
 * - Nullable generics
 * - Generics with collections
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SAMGenericClassTest {

    @Test
    fun `GIVEN SAM with single generic WHEN using reified fake THEN should be type-safe`() {
        // Given
        val transformer = fakeTransformer<String> {
            transform { input -> input.uppercase() }
        }

        // When
        val result = transformer.transform("hello")

        // Then
        assertEquals("HELLO", result, "Should transform string")
        assertIs<String>(result, "Should maintain type safety")
    }

    @Test
    fun `GIVEN SAM with two generics WHEN configuring THEN should convert types`() {
        // Given
        val converter = fakeConverter<String, Int> {
            convert { input -> input.length }
        }

        // When
        val result = converter.convert("hello")

        // Then
        assertEquals(5, result, "Should convert string to int")
        assertIs<Int>(result, "Should return correct type")
    }

    @Test
    fun `GIVEN SAM with generic constraint WHEN using fake THEN should respect bounds`() {
        // Given
        val processor = fakeComparableProcessor<String> {
            process { item -> item.uppercase() }
        }

        // When
        val result = processor.process("test")

        // Then
        assertEquals("TEST", result, "Should process comparable type")
    }

    @Test
    fun `GIVEN SAM with multiple constraints WHEN creating fake THEN should compile`() {
        // Given
        val handler = fakeMultiConstraintHandler<String> {
            handle { item -> item.length }
        }

        // When
        val result = handler.handle("hello")

        // Then
        assertEquals(5, result, "Should handle multi-constrained type")
    }

    @Test
    fun `GIVEN SAM with nullable generic WHEN transforming null THEN should handle correctly`() {
        // Given
        val transformer = fakeNullableTransformer<String> {
            transform { input -> input?.uppercase() }
        }

        // When
        val nonNullResult = transformer.transform("hello")
        val nullResult = transformer.transform(null)

        // Then
        assertEquals("HELLO", nonNullResult, "Should transform non-null value")
        assertNull(nullResult, "Should return null for null input")
    }

    @Test
    fun `GIVEN SAM with List generic WHEN mapping THEN should transform all elements`() {
        // Given
        val mapper = fakeListMapper<Int, String> {
            map { items -> items.map { it.toString() } }
        }

        // When
        val result = mapper.map(listOf(1, 2, 3))

        // Then
        assertEquals(listOf("1", "2", "3"), result, "Should map all elements")
    }

    @Test
    fun `GIVEN SAM with Result generic WHEN handling THEN should wrap in Result`() {
        // Given
        val handler = fakeResultHandler<String> {
            handle { input -> Result.success(input.uppercase()) }
        }

        // When
        val result = handler.handle("hello")

        // Then
        assertTrue(result.isSuccess, "Should return success")
        assertEquals("HELLO", result.getOrNull(), "Should contain transformed value")
    }

    @Test
    fun `GIVEN SAM with suspend generic WHEN transforming async THEN should work in coroutines`() = runTest {
        // Given
        val transformer = fakeAsyncTransformer<Int> {
            transform { input -> input * 2 }
        }

        // When
        val result = transformer.transform(21)

        // Then
        assertEquals(42, result, "Should transform value asynchronously")
    }

    @Test
    fun `GIVEN generic SAM with identity function WHEN not configured THEN should return default`() {
        // Given - identity transformer without configuration
        val transformer = fakeTransformer<String>()

        // When
        val result = transformer.transform("test")

        // Then
        // Should have identity behavior or return input as-is
        assertEquals("test", result, "Should have identity default behavior")
    }

    @Test
    fun `GIVEN generic SAM with complex type WHEN using custom type THEN should work`() {
        // Given
        data class Person(val name: String, val age: Int)
        val transformer = fakeTransformer<Person> {
            transform { person -> person.copy(age = person.age + 1) }
        }

        // When
        val result = transformer.transform(Person("Alice", 30))

        // Then
        assertEquals(Person("Alice", 31), result, "Should transform custom type")
    }
}
