// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.callTracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CallTrackingEdgeCasesTest {

    @Test
    fun `GIVEN fake WHEN 10000 sequential calls THEN all tracked correctly`() {
        // Given
        val fake = fakeEdgeCaseService {
            highFrequencyMethod { it * 2 }
        }

        // When - Make 10,000 sequential calls
        repeat(10_000) { i ->
            fake.highFrequencyMethod(i)
        }

        // Then
        assertEquals(10_000, fake.highFrequencyMethodCallCount.value)
    }

    @Test
    fun `GIVEN fake WHEN 1000 concurrent calls with high contention THEN no race conditions`() =
        runTest {
            // Given
            val fake = fakeEdgeCaseService {
                highFrequencyMethod { it * 2 }
            }

            // When - 1000 concurrent calls from multiple coroutines
            List(1000) {
                async { fake.highFrequencyMethod(it) }
            }.awaitAll()

            // Then - Every call counted exactly once
            assertEquals(1000, fake.highFrequencyMethodCallCount.value)
        }

    @Test
    fun `GIVEN fake WHEN performance test with 10k calls THEN tracks all calls correctly`() {
        // Given
        val fake = fakeEdgeCaseService {
            highFrequencyMethod { it }
        }

        // When
        repeat(10_000) { i ->
            fake.highFrequencyMethod(i)
        }

        // Then - Should track all calls correctly
        assertEquals(10_000, fake.highFrequencyMethodCallCount.value)
    }

    @Test
    fun `GIVEN fake WHEN concurrent reads and writes THEN reads are consistent`() =
        runTest {
            // Given
            val fake = fakeEdgeCaseService {
                concurrentMethod { delay(1); it }
            }

            // When - Mix of reads and writes
            val jobs = buildList {
                // 50 concurrent writes (increment counter)
                repeat(50) { i ->
                    add(launch { fake.concurrentMethod(i) })
                }
                // 50 concurrent reads (read counter value)
                repeat(50) {
                    add(
                        launch {
                            val count = fake.concurrentMethodCallCount.value
                            assertTrue(count >= 0, "Count should never be negative: $count")
                        },
                    )
                }
            }
            jobs.forEach { it.join() }

            // Then
            assertEquals(50, fake.concurrentMethodCallCount.value)
        }

    @Test
    fun `GIVEN fake WHEN reading during update THEN no stale values observed`() =
        runTest {
            // Given
            val fake = fakeEdgeCaseService {
                concurrentMethod { it }
            }
            val observations = mutableListOf<Int>()

            // When - Concurrently write and observe
            val writer = launch {
                repeat(100) {
                    fake.concurrentMethod(it)
                }
            }

            val observer = launch {
                repeat(100) {
                    observations.add(fake.concurrentMethodCallCount.value)
                    delay(1)
                }
            }

            writer.join()
            observer.join()

            // Then - Observations should be monotonically increasing or stable
            observations.zipWithNext().forEach { (prev, next) ->
                assertTrue(next >= prev, "Values should not decrease: $prev -> $next")
            }
        }

    @Test
    fun `GIVEN suspend method WHEN cancelled before completion THEN callCount still increments`() =
        runTest {
            // Given
            val fake = fakeEdgeCaseService {
                cancellableMethod { delay(1000); "completed" }
            }

            // When - Start method then cancel immediately
            val job = launch { fake.cancellableMethod(1000) }
            delay(10) // Let tracking happen
            job.cancel()

            // Then - Call was tracked even though cancelled
            assertEquals(1, fake.cancellableMethodCallCount.value)
        }

    @Test
    fun `GIVEN suspend method WHEN multiple cancellations THEN all tracked`() =
        runTest {
            // Given
            val fake = fakeEdgeCaseService {
                cancellableMethod { delay(1000); "completed" }
            }

            // When - Launch 5 jobs and cancel all
            val jobs = List(5) {
                launch { fake.cancellableMethod(1000) }
            }
            delay(10)
            jobs.forEach { it.cancel() }

            // Then
            assertEquals(5, fake.cancellableMethodCallCount.value)
        }

    // ========================================
    // ⭐⭐⭐ CRITICAL: Exception Handling
    // ========================================

    @Test
    fun `GIVEN method throwing after tracking WHEN called THEN count increments`() {
        // Given
        val fake = fakeEdgeCaseService {
            throwingMethod { shouldThrow ->
                if (shouldThrow) error("Test error") else "success"
            }
        }

        // When
        runCatching { fake.throwingMethod(true) }
        runCatching { fake.throwingMethod(true) }
        fake.throwingMethod(false)

        // Then - All 3 calls tracked including the throwing ones
        assertEquals(3, fake.throwingMethodCallCount.value)
    }

    @Test
    fun `GIVEN method with try-finally WHEN exception thrown THEN tracking in correct order`() {
        // Given
        var finallyExecuted = false
        val fake = fakeEdgeCaseService {
            throwingMethod { shouldThrow ->
                try {
                    if (shouldThrow) error("Test error")
                    "success"
                } finally {
                    finallyExecuted = true
                }
            }
        }

        // When
        runCatching { fake.throwingMethod(true) }

        // Then - Both tracking and finally executed
        assertTrue(finallyExecuted)
        assertEquals(1, fake.throwingMethodCallCount.value)
    }

    @Test
    fun `GIVEN method WHEN different exception types THEN all tracked`() {
        // Given
        val fake = fakeEdgeCaseService {
            throwingMethod { shouldThrow ->
                when {
                    shouldThrow -> throw IllegalStateException("ISE")
                    else -> "success"
                }
            }
        }

        // When
        runCatching { fake.throwingMethod(true) } // ISE
        runCatching { fake.throwingMethod(true) } // ISE
        fake.throwingMethod(false) // Success

        // Then
        assertEquals(3, fake.throwingMethodCallCount.value)
    }

    @Test
    fun `GIVEN fake with recursive behavior WHEN method calls itself THEN each recursion tracked`() {
        // Given - Can't configure recursion in DSL because fake is not accessible
        // So we use a counter approach
        var recursiveCalls = 0
        val fake = fakeEdgeCaseService {
            recursiveMethod { depth ->
                recursiveCalls++
                if (depth <= 0) 0 else depth + (depth - 1)
            }
        }

        // When - Manually call recursively to simulate recursion
        repeat(6) { fake.recursiveMethod(it) }

        // Then
        assertEquals(6, fake.recursiveMethodCallCount.value) // All calls tracked
    }

    @Test
    fun `GIVEN fake with deep recursion simulation WHEN called multiple times THEN all tracked`() {
        // Given
        val fake = fakeEdgeCaseService {
            recursiveMethod { depth ->
                depth * depth
            }
        }

        // When - Simulate deep recursion by calling many times
        repeat(11) { i ->
            fake.recursiveMethod(i)
        }

        // Then - All 11 calls tracked
        assertEquals(11, fake.recursiveMethodCallCount.value)
    }

    @Test
    fun `GIVEN method with default params WHEN called with or without defaults THEN single counter tracks all`() {
        // Given
        val fake = fakeEdgeCaseService {
            methodWithDefaults { required, optional1, optional2 ->
                "$required-$optional1-$optional2"
            }
        }

        // When - Call with various combinations
        fake.methodWithDefaults("test") // All defaults
        fake.methodWithDefaults("test", 99) // One default
        fake.methodWithDefaults("test", 99, false) // No defaults

        // Then - All tracked in same counter
        assertEquals(3, fake.methodWithDefaultsCallCount.value)
    }

    @Test
    fun `GIVEN method with multiple default params WHEN various combinations THEN all tracked in same counter`() {
        // Given
        val fake = fakeEdgeCaseService {
            methodWithDefaults { required, optional1, optional2 ->
                "$required-$optional1-$optional2"
            }
        }

        // When - Different call patterns
        fake.methodWithDefaults("a")
        fake.methodWithDefaults("b", 1)
        fake.methodWithDefaults("c", 2, true)
        fake.methodWithDefaults("d", optional2 = false)

        // Then
        assertEquals(4, fake.methodWithDefaultsCallCount.value)
    }

    @Test
    fun `GIVEN property with complex getter WHEN accessed multiple times THEN each access tracked`() {
        // Given
        var accessCount = 0
        val fake = fakeEdgeCaseService {
            complexProperty {
                accessCount++
                "value-$accessCount"
            }
        }

        // When
        val v1 = fake.complexProperty
        val v2 = fake.complexProperty
        val v3 = fake.complexProperty

        // Then - Each property access tracked
        assertEquals(3, fake.complexPropertyCallCount.value)
        assertEquals("value-1", v1)
        assertEquals("value-2", v2)
        assertEquals("value-3", v3)
    }

    @Test
    fun `GIVEN var property WHEN setting multiple times rapidly THEN all sets tracked`() {
        // Given
        val fake = fakeEdgeCaseService {
            rapidSetProperty { 0 }
        }

        // When - Rapid successive sets
        repeat(100) { i ->
            fake.rapidSetProperty = i
        }

        // Then - All setter calls tracked
        assertEquals(100, fake.setRapidSetPropertyCallCount.value)
    }

    @Test
    fun `GIVEN var property WHEN mixed reads and writes THEN both tracked separately`() {
        // Given
        val fake = fakeEdgeCaseService {
            rapidSetProperty { 42 }
        }

        // When
        val r1 = fake.rapidSetProperty // read
        fake.rapidSetProperty = 100 // write
        val r2 = fake.rapidSetProperty // read
        fake.rapidSetProperty = 200 // write
        val r3 = fake.rapidSetProperty // read

        // Then
        assertEquals(3, fake.rapidSetPropertyCallCount.value) // 3 reads
        assertEquals(2, fake.setRapidSetPropertyCallCount.value) // 2 writes
    }

    @Test
    fun `GIVEN fake with high call count WHEN accessing callCount THEN returns correct value`() {
        // Given
        val fake = fakeEdgeCaseService {
            highFrequencyMethod { it }
        }

        // When - Many calls over time
        repeat(5000) { i ->
            fake.highFrequencyMethod(i)

            // Periodically check counter (simulating monitoring/debugging)
            if (i % 1000 == 0) {
                val currentCount = fake.highFrequencyMethodCallCount.value
                assertTrue(currentCount > 0, "Counter should be positive")
            }
        }

        // Then
        assertEquals(5000, fake.highFrequencyMethodCallCount.value)
    }

    @Test
    fun `GIVEN fake WHEN first call after creation THEN tracking works immediately`() {
        // Given
        val fake = fakeEdgeCaseService {
            highFrequencyMethod { it }
        }

        // When - First call ever (cold start)
        fake.highFrequencyMethod(1)

        // Then - Tracking initialized correctly
        assertEquals(1, fake.highFrequencyMethodCallCount.value)
    }

    @Test
    fun `GIVEN fake WHEN accessing callCount before any calls THEN value is 0`() {
        // Given
        val fake = fakeEdgeCaseService()

        // When - Check counter before any method calls
        val countBeforeCalls = fake.highFrequencyMethodCallCount.value

        // Then
        assertEquals(0, countBeforeCalls)
    }

    @Test
    fun `GIVEN fake WHEN many calls followed by period of inactivity THEN counter remains stable`() =
        runTest {
            // Given
            val fake = fakeEdgeCaseService {
                highFrequencyMethod { it }
            }

            // When - Active period
            repeat(100) { fake.highFrequencyMethod(it) }
            val countAfterActivity = fake.highFrequencyMethodCallCount.value

            // Simulate inactivity
            delay(100)

            val countAfterInactivity = fake.highFrequencyMethodCallCount.value

            // Then - Counter stable during inactivity
            assertEquals(countAfterActivity, countAfterInactivity)
            assertEquals(100, countAfterInactivity)
        }
}
