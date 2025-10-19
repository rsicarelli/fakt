// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.callTracking

import com.rsicarelli.fakt.samples.singlemodule.models.User
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive call tracking tests using MutableStateFlow.
 *
 * Tests follow GIVEN-WHEN-THEN BDD pattern and validate:
 * - Basic call counting
 * - Thread-safety with concurrent calls
 * - Suspend function tracking
 * - Generic method tracking
 * - Nullable type handling
 * - Custom domain types
 * - Property access tracking (getter/setter)
 * - StateFlow observation
 * - Edge cases
 */
class CallTrackingTest {
    // ========================================
    // 1. Basic Call Counting
    // ========================================

    @Test
    fun `GIVEN fake with call tracking WHEN method called once THEN callCount should be 1`() {
        // Given
        val fake =
            fakeTrackedService {
                simpleMethod { "result" }
            }

        // When
        fake.simpleMethod()

        // Then
        assertEquals(1, fake.simpleMethodCallCount.value)
    }

    @Test
    fun `GIVEN fake with call tracking WHEN method called 3 times THEN callCount should be 3`() {
        // Given
        val fake =
            fakeTrackedService {
                simpleMethod { "result" }
            }

        // When
        fake.simpleMethod()
        fake.simpleMethod()
        fake.simpleMethod()

        // Then
        assertEquals(3, fake.simpleMethodCallCount.value)
    }

    @Test
    fun `GIVEN fake with no calls WHEN checking callCount THEN should be 0`() {
        // Given
        val fake = fakeTrackedService()

        // When
        val count = fake.simpleMethodCallCount.value

        // Then
        assertEquals(0, count)
    }

    // ========================================
    // 2. Concurrent Calls (Thread-Safety)
    // ========================================

    @Test
    fun `GIVEN fake WHEN 100 concurrent coroutines call method THEN callCount should be 100`() =
        runTest {
            // Given
            val fake =
                fakeTrackedService {
                    simpleMethod { "result" }
                }

            // When - Launch 100 concurrent coroutines
            List(100) {
                async { fake.simpleMethod() }
            }.awaitAll()

            // Then
            assertEquals(100, fake.simpleMethodCallCount.value)
        }

    @Test
    fun `GIVEN fake WHEN multiple suspend methods called concurrently THEN all counts accurate`() =
        runTest {
            // Given
            val fake =
                fakeTrackedService {
                    asyncMethod { "async-result" }
                    batchProcess { items -> items.map { it.uppercase() } }
                }

            // When - Mix of concurrent calls
            val jobs =
                buildList {
                    repeat(50) { add(launch { fake.asyncMethod() }) }
                    repeat(30) { add(launch { fake.batchProcess(listOf("a", "b")) }) }
                }
            jobs.forEach { it.join() }

            // Then
            assertEquals(50, fake.asyncMethodCallCount.value)
            assertEquals(30, fake.batchProcessCallCount.value)
        }

    // ========================================
    // 3. Suspend Functions Tracking
    // ========================================

    @Test
    fun `GIVEN suspend method WHEN called multiple times THEN callCount increments correctly`() =
        runTest {
            // Given
            val fake =
                fakeTrackedService {
                    asyncMethod { "async-result" }
                }

            // When
            fake.asyncMethod()
            fake.asyncMethod()
            fake.asyncMethod()

            // Then
            assertEquals(3, fake.asyncMethodCallCount.value)
        }

    @Test
    fun `GIVEN async batch operations WHEN concurrent execution THEN tracking remains accurate`() =
        runTest {
            // Given
            val fake =
                fakeTrackedService {
                    batchProcess { items -> items }
                }

            // When - Process 20 batches concurrently
            List(20) {
                async {
                    fake.batchProcess(listOf("item-$it"))
                }
            }.awaitAll()

            // Then
            assertEquals(20, fake.batchProcessCallCount.value)
        }

    // ========================================
    // 4. Generic Methods Tracking
    // ========================================

    @Test
    fun `GIVEN method with generics WHEN called with different types THEN callCount tracks all invocations`() {
        // Given
        val fake =
            fakeTrackedService {
                genericMethod<Any?> { it }
            }

        // When - Call with different types
        fake.genericMethod("string")
        fake.genericMethod(42)
        fake.genericMethod(true)

        // Then - Single counter tracks all invocations regardless of type
        assertEquals(3, fake.genericMethodCallCount.value)
    }

    @Test
    fun `GIVEN method-level generics WHEN multiple type instantiations THEN single counter tracks all`() =
        runTest {
            // Given
            val fake =
                fakeTrackedService {
                    asyncGenericMethod<Any?> { it }
                }

            // When
            fake.asyncGenericMethod("test")
            fake.asyncGenericMethod(123)
            fake.asyncGenericMethod(listOf(1, 2, 3))

            // Then
            assertEquals(3, fake.asyncGenericMethodCallCount.value)
        }

    // ========================================
    // 5. Nullable Types Tracking
    // ========================================

    @Test
    fun `GIVEN method returning nullable WHEN called THEN callCount increments`() {
        // Given
        val fake =
            fakeTrackedService {
                nullableMethod { "result" }
            }

        // When
        fake.nullableMethod()
        fake.nullableMethod()

        // Then
        assertEquals(2, fake.nullableMethodCallCount.value)
    }

    @Test
    fun `GIVEN method with nullable params WHEN passed null THEN tracking works correctly`() {
        // Given
        val fake =
            fakeTrackedService {
                nullableParamMethod { value -> value != null }
            }

        // When
        fake.nullableParamMethod(null)
        fake.nullableParamMethod("not-null")
        fake.nullableParamMethod(null)

        // Then
        assertEquals(3, fake.nullableParamMethodCallCount.value)
    }

    // ========================================
    // 6. Custom Domain Types Tracking
    // ========================================

    @Test
    fun `GIVEN method with User type WHEN called THEN callCount tracks invocations`() {
        // Given
        val fake =
            fakeTrackedService {
                customTypeMethod { user -> user.copy(name = "${user.name}-modified") }
            }

        // When
        val user = User("1", "Alice", "alice@test.com", 25)
        fake.customTypeMethod(user)
        fake.customTypeMethod(user)

        // Then
        assertEquals(2, fake.customTypeMethodCallCount.value)
    }

    @Test
    fun `GIVEN complex return types WHEN invoked THEN tracking accurate`() {
        // Given
        val fake =
            fakeTrackedService {
                methodWithParams { _, _ -> true }
            }

        // When
        fake.methodWithParams("id-1", 10)
        fake.methodWithParams("id-2", 20)
        fake.methodWithParams("id-3", 30)

        // Then
        assertEquals(3, fake.methodWithParamsCallCount.value)
    }

    // ========================================
    // 7. Property Access Tracking
    // ========================================

    @Test
    fun `GIVEN property val WHEN accessed multiple times THEN callCount increments`() {
        // Given
        val fake =
            fakeTrackedService {
                readOnlyProperty { "property-value" }
            }

        // When
        val value1 = fake.readOnlyProperty
        val value2 = fake.readOnlyProperty
        val value3 = fake.readOnlyProperty

        // Then
        assertEquals(3, fake.readOnlyPropertyCallCount.value)
    }

    @Test
    fun `GIVEN property var WHEN read and written THEN separate tracking for getter and setter`() {
        // Given
        val fake =
            fakeTrackedService {
                mutableProperty { 42 }
            }

        // When
        val read1 = fake.mutableProperty // getter
        val read2 = fake.mutableProperty // getter
        fake.mutableProperty = 100 // setter
        fake.mutableProperty = 200 // setter
        val read3 = fake.mutableProperty // getter

        // Then
        assertEquals(3, fake.mutablePropertyCallCount.value) // 3 reads
        assertEquals(2, fake.setMutablePropertyCallCount.value) // 2 writes
    }

    // ========================================
    // 8. StateFlow Observation
    // ========================================

    @Test
    fun `GIVEN fake WHEN observing callCount flow THEN can read current value`() {
        // Given
        val fake =
            fakeTrackedService {
                simpleMethod { "result" }
            }

        // When
        fake.simpleMethod()
        val countFlow = fake.simpleMethodCallCount

        // Then
        assertNotNull(countFlow)
        assertEquals(1, countFlow.value)
    }

    @Test
    fun `GIVEN multiple methods WHEN called THEN can observe all flows independently`() {
        // Given
        val fake =
            fakeTrackedService {
                simpleMethod { "simple" }
                methodWithParams { _, _ -> true }
                nullableMethod { "nullable" }
            }

        // When
        fake.simpleMethod()
        fake.simpleMethod()
        fake.methodWithParams("id", 1)
        fake.nullableMethod()
        fake.nullableMethod()
        fake.nullableMethod()

        // Then - Each flow tracks independently
        assertEquals(2, fake.simpleMethodCallCount.value)
        assertEquals(1, fake.methodWithParamsCallCount.value)
        assertEquals(3, fake.nullableMethodCallCount.value)
    }

    // ========================================
    // 9. Varargs Methods
    // ========================================

    @Test
    fun `GIVEN method with varargs WHEN called with different arg counts THEN tracking works`() {
        // Given
        val fake =
            fakeTrackedService {
                varargsMethod { values -> values.size }
            }

        // When
        fake.varargsMethod("a")
        fake.varargsMethod("a", "b", "c")
        fake.varargsMethod()

        // Then
        assertEquals(3, fake.varargsMethodCallCount.value)
    }

    // ========================================
    // 10. Edge Cases
    // ========================================

    @Test
    fun `GIVEN method throws exception WHEN called THEN callCount still increments`() {
        // Given
        val fake =
            fakeTrackedService {
                simpleMethod { error("Intentional error") }
            }

        // When
        runCatching { fake.simpleMethod() }
        runCatching { fake.simpleMethod() }

        // Then - Counter increments even when method throws
        assertEquals(2, fake.simpleMethodCallCount.value)
    }

    @Test
    fun `GIVEN behavior configured to return different values WHEN called THEN tracking independent of behavior`() {
        // Given
        var counter = 0
        val fake =
            fakeTrackedService {
                simpleMethod {
                    counter++
                    "result-$counter"
                }
            }

        // When
        val result1 = fake.simpleMethod()
        val result2 = fake.simpleMethod()
        val result3 = fake.simpleMethod()

        // Then
        assertEquals("result-1", result1)
        assertEquals("result-2", result2)
        assertEquals("result-3", result3)
        assertEquals(3, fake.simpleMethodCallCount.value)
    }

    @Test
    fun `GIVEN fake with default behaviors WHEN methods called THEN tracking works without configuration`() =
        runTest {
            // Given
            val fake = fakeTrackedService()

            // When - Call with defaults
            fake.simpleMethod()
            fake.asyncMethod()
            val prop = fake.readOnlyProperty

            // Then
            assertEquals(1, fake.simpleMethodCallCount.value)
            assertEquals(1, fake.asyncMethodCallCount.value)
            assertEquals(1, fake.readOnlyPropertyCallCount.value)
        }

    @Test
    fun `GIVEN multiple fakes WHEN each tracks calls independently THEN no cross-contamination`() {
        // Given
        val fake1 = fakeTrackedService { simpleMethod { "fake1" } }
        val fake2 = fakeTrackedService { simpleMethod { "fake2" } }

        // When
        fake1.simpleMethod()
        fake1.simpleMethod()
        fake2.simpleMethod()

        // Then - Independent tracking
        assertEquals(2, fake1.simpleMethodCallCount.value)
        assertEquals(1, fake2.simpleMethodCallCount.value)
    }

    @Test
    fun `GIVEN zero calls WHEN all methods uncalled THEN all counters are zero`() {
        // Given
        val fake = fakeTrackedService()

        // When - No calls made

        // Then
        assertEquals(0, fake.simpleMethodCallCount.value)
        assertEquals(0, fake.asyncMethodCallCount.value)
        assertEquals(0, fake.genericMethodCallCount.value)
        assertEquals(0, fake.methodWithParamsCallCount.value)
        assertEquals(0, fake.readOnlyPropertyCallCount.value)
        assertEquals(0, fake.mutablePropertyCallCount.value)
        assertEquals(0, fake.setMutablePropertyCallCount.value)
        assertEquals(0, fake.nullableMethodCallCount.value)
        assertEquals(0, fake.customTypeMethodCallCount.value)
        assertEquals(0, fake.batchProcessCallCount.value)
        assertEquals(0, fake.varargsMethodCallCount.value)
        assertEquals(0, fake.nullableParamMethodCallCount.value)
        assertEquals(0, fake.asyncGenericMethodCallCount.value)
    }
}
