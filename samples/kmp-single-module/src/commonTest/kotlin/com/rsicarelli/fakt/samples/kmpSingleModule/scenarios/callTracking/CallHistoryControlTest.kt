// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.callTracking

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for call history control feature.
 *
 * Tests the three CallHistoryMode options:
 * - DISABLED: No call tracking generated
 * - ENABLED: Call tracking always generated
 * - DEFAULT: Follows plugin configuration (enabled by default)
 */
class CallHistoryControlTest {
    // ==========================================
    // CallHistoryMode.DISABLED Tests
    // ==========================================

    @Test
    fun `GIVEN LightweightService WHEN configured THEN fake can be used without call tracking`() {
        // GIVEN: A lightweight service with call history disabled
        val fake = fakeLightweightService {
            process { value -> "processed: $value" }
        }

        // WHEN: Using the fake
        val result = fake.process("test")

        // THEN: It works correctly (no call tracking API available)
        assertEquals("processed: test", result)
        // Note: fake.processCalls.value.size would not compile as it's not generated
    }

    @Test
    fun `GIVEN LightweightService WHEN suspend method called THEN works without call tracking`() =
        runTest {
            // GIVEN: A lightweight service
            val fake = fakeLightweightService {
                asyncProcess { items -> items.map { it.uppercase() } }
            }

            // WHEN: Calling the suspend method
            val result = fake.asyncProcess(listOf("a", "b", "c"))

            // THEN: It works correctly
            assertEquals(listOf("A", "B", "C"), result)
        }

    @Test
    fun `GIVEN LightweightService WHEN property accessed THEN works without call tracking`() {
        // GIVEN: A lightweight service
        val fake = fakeLightweightService {
            status { "ready" }
        }

        // WHEN: Accessing the property
        val status = fake.status

        // THEN: It works correctly
        assertEquals("ready", status)
    }

    // ==========================================
    // CallHistoryMode.ENABLED Tests
    // ==========================================

    @Test
    fun `GIVEN FullyTrackedService WHEN method called THEN call tracking is available`() {
        // GIVEN: A fully tracked service with call history enabled
        val fake = fakeFullyTrackedService {
            execute { command -> command.isNotEmpty() }
        }

        // WHEN: Calling the method multiple times
        fake.execute("command1")
        fake.execute("command2")
        fake.execute("")

        // THEN: Call tracking is available
        assertEquals(3, fake.executeCalls.value.size)
    }

    @Test
    fun `GIVEN FullyTrackedService WHEN suspend method called THEN call tracking is available`() =
        runTest {
            // GIVEN: A fully tracked service
            val fake = fakeFullyTrackedService {
                asyncExecute { commands -> commands.map { it.isNotEmpty() } }
            }

            // WHEN: Calling the suspend method
            fake.asyncExecute(listOf("cmd1"))
            fake.asyncExecute(listOf("cmd2", "cmd3"))

            // THEN: Call tracking is available
            assertEquals(2, fake.asyncExecuteCalls.value.size)
        }

    @Test
    fun `GIVEN FullyTrackedService WHEN property accessed THEN call tracking is available`() {
        // GIVEN: A fully tracked service
        val fake = fakeFullyTrackedService {
            isReady { true }
        }

        // WHEN: Accessing the property multiple times
        repeat(5) { fake.isReady }

        // THEN: Call tracking is available
        assertEquals(5, fake.isReadyCalls.value.size)
    }

    // ==========================================
    // CallHistoryMode.DEFAULT Tests (follows plugin default)
    // ==========================================

    @Test
    fun `GIVEN DefaultTrackedService WHEN method called THEN call tracking follows plugin default`() {
        // GIVEN: A service with default call history mode
        // By default, plugin has call history ENABLED
        val fake = fakeDefaultTrackedService {
            doWork { input -> "result: $input" }
        }

        // WHEN: Calling the method
        fake.doWork(1)
        fake.doWork(2)

        // THEN: Call tracking is available (plugin default is enabled)
        assertEquals(2, fake.doWorkCalls.value.size)
    }

    @Test
    fun `GIVEN DefaultTrackedService WHEN property accessed THEN call tracking follows plugin default`() {
        // GIVEN: A service with default call history mode
        val fake = fakeDefaultTrackedService {
            workCount { 42 }
        }

        // WHEN: Accessing the property
        val value = fake.workCount

        // THEN: Value is correct and tracking works
        assertEquals(42, value)
        assertEquals(1, fake.workCountCalls.value.size)
    }

    // ==========================================
    // Mixed Usage Tests
    // ==========================================

    @Test
    fun `GIVEN mixed services WHEN used together THEN each respects its configuration`() =
        runTest {
            // GIVEN: Different services with different call history configurations
            val lightweight = fakeLightweightService {
                process { "light" }
            }
            val tracked = fakeFullyTrackedService {
                execute { true }
            }

            // WHEN: Using both services
            lightweight.process("input")
            lightweight.process("input2")
            tracked.execute("cmd")

            // THEN: Tracked service has call count, lightweight doesn't
            assertEquals(1, tracked.executeCalls.value.size)
            // lightweight.processCalls.value.size would not compile
        }
}
