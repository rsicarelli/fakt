// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.propertiesAndMethods
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive test for EventProcessor fake generation.
 *
 * Validates:
 * - Lambda/function type parameters
 * - Callback patterns
 * - Predicate functions
 * - Suspend functions with lambdas
 */
class EventProcessorTest {
    @Test
    fun `GIVEN EventProcessor fake WHEN processing string THEN should execute processor function`() {
        // Given
        val fake =
            fakeEventProcessor {
                processString { item, processor ->
                    processor(item.uppercase())
                }
            }

        // When
        val result = fake.processString("hello") { it.reversed() }

        // Then
        assertEquals("OLLEH", result)
    }

    @Test
    fun `GIVEN EventProcessor fake WHEN processing int THEN should execute processor function`() {
        // Given
        val fake =
            fakeEventProcessor {
                processInt { item, processor ->
                    processor(item * 2)
                }
            }

        // When
        val result = fake.processInt(21) { "Number: $it" }

        // Then
        assertEquals("Number: 42", result)
    }

    @Test
    fun `GIVEN EventProcessor fake WHEN filtering THEN should use predicate`() {
        // Given
        val fake =
            fakeEventProcessor {
                filter { items, predicate ->
                    items.filter(predicate)
                }
            }

        // When
        val result = fake.filter(listOf("apple", "banana", "apricot")) { it.startsWith("a") }

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("apple"))
        assertTrue(result.contains("apricot"))
    }

    @Test
    fun `GIVEN EventProcessor fake WHEN onComplete callback THEN should execute callback`() {
        // Given
        var callbackExecuted = false
        val fake =
            fakeEventProcessor {
                onComplete { callback ->
                    callback()
                }
            }

        // When
        fake.onComplete {
            callbackExecuted = true
        }

        // Then
        assertTrue(callbackExecuted)
    }

    @Test
    fun `GIVEN EventProcessor fake WHEN onError handler THEN should pass exception`() {
        // Given
        var capturedException: Exception? = null
        val fake =
            fakeEventProcessor {
                onError { errorHandler ->
                    errorHandler(Exception("Test error"))
                }
            }

        // When
        fake.onError { ex ->
            capturedException = ex
        }

        // Then
        assertEquals("Test error", capturedException?.message)
    }

    @Test
    fun `GIVEN EventProcessor fake WHEN async processing THEN should handle suspend processor`() =
        runTest {
            // Given
            val fake =
                fakeEventProcessor {
                    processAsync { item, processor ->
                        processor(item.uppercase())
                    }
                }

            // When
            val result = fake.processAsync("async") { it.reversed() }

            // Then
            assertEquals("CNYSA", result)
        }

    @Test
    fun `GIVEN EventProcessor fake WHEN using defaults THEN should have sensible defaults`() =
        runTest {
            // Given
            val fake = fakeEventProcessor()

            // When
            val processedString = fake.processString("test") { it }
            val processedInt = fake.processInt(42) { "result" }
            val filtered = fake.filter(listOf("a", "b")) { true }
            fake.onComplete { } // Should not throw
            fake.onError { } // Should not throw
            val asyncResult = fake.processAsync("async") { it }

            // Then
            assertEquals("", processedString) // Default: empty string
            assertEquals("", processedInt) // Default: empty string
            assertTrue(filtered.isEmpty()) // Default: empty list
            assertEquals("", asyncResult) // Default: empty string
        }
}
