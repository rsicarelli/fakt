// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.generics_method_level
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for DataProcessor - Method-level generics only (no class-level generics).
 *
 * Covers:
 * - Method-level generic type parameters: <T> process(item: T): T
 * - Method-level transformation: <R> transform(input: String): R
 * - Non-generic methods
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataProcessorTest {
    @Test
    fun `GIVEN data processor WHEN processing string THEN should return processed string`() {
        // Given
        val processor =
            fakeDataProcessor {
                process { item: String -> item }
            }

        // When
        val result: String = processor.process("test")

        // Then
        assertEquals("test", result, "Should process string with identity function")
    }

    @Test
    fun `GIVEN data processor WHEN processing int THEN should return processed int`() {
        // Given
        val processor =
            fakeDataProcessor {
                process { item: Int -> item }
            }

        // When
        val result: Int = processor.process(42)

        // Then
        assertEquals(42, result, "Should process int with identity function")
    }

    @Test
    fun `GIVEN data processor WHEN transforming to int THEN should return int`() {
        // Given
        val processor =
            fakeDataProcessor {
                transform { input -> input.length }
            }

        // When
        val result: Int = processor.transform("hello")

        // Then
        assertEquals(5, result, "Should transform string to its length")
    }

    @Test
    fun `GIVEN data processor WHEN transforming to list THEN should return list`() {
        // Given
        val processor =
            fakeDataProcessor {
                transform { input -> input.split(",") }
            }

        // When
        val result: List<String> = processor.transform("a,b,c")

        // Then
        assertEquals(listOf("a", "b", "c"), result, "Should transform string to list")
    }

    @Test
    fun `GIVEN data processor WHEN getting data THEN should return configured data`() {
        // Given
        val processor =
            fakeDataProcessor {
                getData { "configured-data" }
            }

        // When
        val result = processor.getData()

        // Then
        assertEquals("configured-data", result, "Should return configured data")
    }

    @Test
    fun `GIVEN data processor WHEN using all methods THEN should work correctly`() {
        // Given
        val processor =
            fakeDataProcessor {
                process { item: Any? -> item } // Identity works for any type
                transform { input -> input.uppercase() }
                getData { "base-data" }
            }

        // When & Then
        assertEquals("test", processor.process<String>("test"))
        assertEquals(42, processor.process<Int>(42))
        assertEquals("HELLO", processor.transform<String>("hello"))
        assertEquals("base-data", processor.getData())
    }
}
