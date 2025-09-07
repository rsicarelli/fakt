// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

/**
 * Tests for FakeRuntime internal functionality.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover scope management, instance creation, and call tracking.
 */
class FakeRuntimeTest {

    @BeforeTest
    fun setUp() {
        // Clean up any existing state before each test
        FakeRuntime.instanceRegistry.clear()
        FakeRuntime.callTracker.clear()
        FakeRuntime.setScope("test")
    }

    @AfterTest
    fun tearDown() {
        // Clean up state after each test
        FakeRuntime.instanceRegistry.clear()
        FakeRuntime.callTracker.clear()
    }

    @Test
    fun `GIVEN concurrent mode enabled WHEN getting instances THEN should always create new instances`() {
        // Given: Concurrent mode is enabled
        var creationCount = 0
        val factory = {
            creationCount++
            TestService("instance-$creationCount")
        }

        // When: Getting multiple instances with concurrent = true
        val instance1 = FakeRuntime.getInstance("TestService", concurrent = true, factory)
        val instance2 = FakeRuntime.getInstance("TestService", concurrent = true, factory)
        val instance3 = FakeRuntime.getInstance("TestService", concurrent = true, factory)

        // Then: Each call should create a new instance
        assertEquals(3, creationCount, "Factory should be called 3 times")
        assertNotSame(instance1, instance2, "Instance1 and instance2 should be different objects")
        assertNotSame(instance2, instance3, "Instance2 and instance3 should be different objects")
        assertEquals("instance-1", instance1.id)
        assertEquals("instance-2", instance2.id)
        assertEquals("instance-3", instance3.id)
    }

    @Test
    fun `GIVEN concurrent mode disabled WHEN getting instances in same scope THEN should reuse same instance`() {
        // Given: Concurrent mode is disabled and same scope
        var creationCount = 0
        val factory = {
            creationCount++
            TestService("singleton-$creationCount")
        }
        FakeRuntime.setScope("test-scope")

        // When: Getting multiple instances with concurrent = false
        val instance1 = FakeRuntime.getInstance("TestService", concurrent = false, factory)
        val instance2 = FakeRuntime.getInstance("TestService", concurrent = false, factory)
        val instance3 = FakeRuntime.getInstance("TestService", concurrent = false, factory)

        // Then: Should reuse the same instance
        assertEquals(1, creationCount, "Factory should only be called once")
        assertSame(instance1, instance2, "Instance1 and instance2 should be same object")
        assertSame(instance2, instance3, "Instance2 and instance3 should be same object")
        assertEquals("singleton-1", instance1.id)
    }

    @Test
    fun `GIVEN concurrent mode disabled WHEN getting instances in different scopes THEN should create different instances`() {
        // Given: Concurrent mode is disabled but different scopes
        var creationCount = 0
        val factory = {
            creationCount++
            TestService("scoped-$creationCount")
        }

        // When: Getting instances in different scopes with concurrent = false
        FakeRuntime.setScope("scope1")
        val instance1 = FakeRuntime.getInstance("TestService", concurrent = false, factory)

        FakeRuntime.setScope("scope2")
        val instance2 = FakeRuntime.getInstance("TestService", concurrent = false, factory)

        // Then: Should create different instances for different scopes
        assertEquals(2, creationCount, "Factory should be called twice for different scopes")
        assertNotSame(instance1, instance2, "Instances from different scopes should be different")
        assertEquals("scoped-1", instance1.id)
        assertEquals("scoped-2", instance2.id)
    }

    @Test
    fun `GIVEN scope with instances WHEN clearing scope THEN should remove scope instances`() {
        // Given: A scope with instances
        val factory = { TestService("test") }
        FakeRuntime.setScope("clearable-scope")
        FakeRuntime.getInstance("TestService", concurrent = false, factory)

        assertTrue(FakeRuntime.instanceRegistry.containsKey("clearable-scope"), "Scope should exist before clearing")

        // When: Clearing the scope
        FakeRuntime.clearScope("clearable-scope")

        // Then: Scope should be removed
        assertTrue(!FakeRuntime.instanceRegistry.containsKey("clearable-scope"), "Scope should be removed after clearing")
    }

    @Test
    fun `GIVEN call tracking WHEN recording method calls THEN should store call records correctly`() {
        // Given: Call tracking is enabled
        val instanceId = "test-instance"
        val methodName = "testMethod"
        val arguments: Array<Any?> = arrayOf("arg1", 42, true)
        val returnValue = "result"
        val timestamp = 1234567890L

        // When: Recording a method call
        FakeRuntime.recordCall(instanceId, methodName, arguments, returnValue, timestamp)

        // Then: Call should be recorded correctly
        val key = "$instanceId.$methodName"
        assertTrue(FakeRuntime.callTracker.containsKey(key), "Call tracker should contain the method key")

        val calls = FakeRuntime.callTracker[key]!!
        assertEquals(1, calls.size, "Should have recorded one call")

        val call = calls.first()
        assertEquals(methodName, call.methodName)
        assertEquals(listOf("arg1", 42, true), call.arguments)
        assertEquals(returnValue, call.returnValue)
        assertEquals(timestamp, call.timestamp)
    }

    @Test
    fun `GIVEN multiple calls to same method WHEN recording calls THEN should store all calls in order`() {
        // Given: Multiple calls to the same method
        val instanceId = "multi-call-instance"
        val methodName = "repeatedMethod"

        // When: Recording multiple calls
        FakeRuntime.recordCall(instanceId, methodName, arrayOf("first"), "result1", 1000L)
        FakeRuntime.recordCall(instanceId, methodName, arrayOf("second"), "result2", 2000L)
        FakeRuntime.recordCall(instanceId, methodName, arrayOf("third"), "result3", 3000L)

        // Then: All calls should be stored in order
        val key = "$instanceId.$methodName"
        val calls = FakeRuntime.callTracker[key]!!

        assertEquals(3, calls.size, "Should have recorded three calls")
        assertEquals(listOf("first"), calls[0].arguments)
        assertEquals(listOf("second"), calls[1].arguments)
        assertEquals(listOf("third"), calls[2].arguments)
        assertEquals("result1", calls[0].returnValue)
        assertEquals("result2", calls[1].returnValue)
        assertEquals("result3", calls[2].returnValue)
    }

    // Helper class for testing
    private data class TestService(val id: String)
}
