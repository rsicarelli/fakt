// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.callTracking

import com.rsicarelli.fakt.samples.kmpSingleModule.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for call history verification feature.
 *
 * Demonstrates the scoped DSL verification API:
 * - Data classes for recording call arguments
 * - Verifier classes with assertion methods
 * - Scoped verify functions (verifyMethodName { ... })
 */
class CallHistoryVerificationTest {

    @Test
    fun `GIVEN tracked service WHEN method called multiple times THEN verifies call history`() {
        // GIVEN
        val fake = fakeTrackedService {
            methodWithParams { _, _ -> true }
        }

        // WHEN
        fake.methodWithParams("user-123", 5)
        fake.methodWithParams("user-456", 10)
        fake.methodWithParams("user-123", 15)

        // THEN
        fake.verifyMethodWithParams {
            assertTrue(wasCalledTimes(3))
            assertTrue(wasCalledWith("user-123", 5))
            assertTrue(wasCalledWith("user-456", 10))
            assertTrue(wasCalledWith("user-123", 15))
            assertFalse(wasNeverCalled())
        }
    }

    @Test
    fun `GIVEN tracked service WHEN method never called THEN verifies empty history`() {
        // GIVEN
        val fake = fakeTrackedService {
            methodWithParams { _, _ -> true }
        }

        // WHEN - not calling the method

        // THEN
        fake.verifyMethodWithParams {
            assertTrue(wasNeverCalled())
            assertTrue(wasCalledTimes(0))
            assertEquals(null, lastOrNull)
            assertTrue(all.isEmpty())
        }
    }

    @Test
    fun `GIVEN tracked service WHEN accessing individual calls THEN returns correct data`() {
        // GIVEN
        val fake = fakeTrackedService {
            methodWithParams { _, _ -> true }
        }

        // WHEN
        fake.methodWithParams("first", 1)
        fake.methodWithParams("second", 2)
        fake.methodWithParams("third", 3)

        // THEN
        fake.verifyMethodWithParams {
            assertEquals("first", first.id)
            assertEquals(1, first.count)

            assertEquals("third", lastOrNull?.id)
            assertEquals(3, lastOrNull?.count)

            assertEquals(3, all.size)
            assertEquals(listOf("first", "second", "third"), all.map { it.id })
        }
    }

    @Test
    fun `GIVEN tracked service with custom type WHEN method called THEN records user correctly`() {
        // GIVEN
        val user1 = User(id = "1", name = "Alice", email = "alice@test.com", age = 25)
        val user2 = User(id = "2", name = "Bob", email = "bob@test.com", age = 30)
        val fake = fakeTrackedService {
            customTypeMethod { it }
        }

        // WHEN
        fake.customTypeMethod(user1)
        fake.customTypeMethod(user2)

        // THEN
        fake.verifyCustomTypeMethod {
            assertTrue(wasCalledTimes(2))
            assertTrue(wasCalledWith(user1))
            assertTrue(wasCalledWith(user2))
            assertEquals(user2, lastOrNull?.user)
        }
    }

    @Test
    fun `GIVEN tracked service WHEN single param method called THEN supports wasCalledInOrder`() {
        // GIVEN
        val fake = fakeTrackedService {
            nullableParamMethod { true }
        }

        // WHEN
        fake.nullableParamMethod("first")
        fake.nullableParamMethod(null)
        fake.nullableParamMethod("third")

        // THEN
        fake.verifyNullableParamMethod {
            assertTrue(wasCalledTimes(3))
            assertTrue(wasCalledInOrder("first", null, "third"))
            assertTrue(neverCalledWith("unknown"))
        }
    }

    @Test
    fun `GIVEN tracked service WHEN verifier returned THEN can do additional assertions`() {
        // GIVEN
        val fake = fakeTrackedService {
            methodWithParams { _, _ -> true }
        }

        // WHEN
        fake.methodWithParams("test", 42)

        // THEN - verify function returns verifier for additional assertions
        val verifier = fake.verifyMethodWithParams {
            assertTrue(wasCalledTimes(1))
        }

        // Additional assertions outside the block
        assertEquals(1, verifier.all.size)
        assertEquals("test", verifier.first.id)
        assertEquals(42, verifier.first.count)
    }

    // region 0-param Method Verification Tests

    @Test
    fun `GIVEN 0-param method WHEN called multiple times THEN verifies call count`() {
        // GIVEN
        val fake = fakeTrackedService {
            simpleMethod { "result" }
        }

        // WHEN
        fake.simpleMethod()
        fake.simpleMethod()
        fake.simpleMethod()

        // THEN
        fake.verifySimpleMethod {
            assertTrue(wasCalledTimes(3))
            assertFalse(wasNeverCalled())
        }
    }

    @Test
    fun `GIVEN 0-param method WHEN never called THEN wasNeverCalled returns true`() {
        // GIVEN
        val fake = fakeTrackedService {
            simpleMethod { "result" }
        }

        // WHEN - not calling the method

        // THEN
        fake.verifySimpleMethod {
            assertTrue(wasNeverCalled())
            assertTrue(wasCalledTimes(0))
        }
    }

    @Test
    fun `GIVEN 0-param async method WHEN called THEN verifies correctly`() {
        // GIVEN
        val fake = fakeTrackedService {
            asyncMethod { "async-result" }
        }

        // WHEN
        kotlinx.coroutines.test.runTest {
            fake.asyncMethod()
            fake.asyncMethod()
        }

        // THEN
        fake.verifyAsyncMethod {
            assertTrue(wasCalledTimes(2))
            assertFalse(wasNeverCalled())
        }
    }

    @Test
    fun `GIVEN nullable return method WHEN called THEN verifies call history`() {
        // GIVEN
        val fake = fakeTrackedService {
            nullableMethod { null }
        }

        // WHEN
        fake.nullableMethod()

        // THEN
        fake.verifyNullableMethod {
            assertTrue(wasCalledTimes(1))
            assertFalse(wasNeverCalled())
        }
    }

    // endregion

    // region Vararg-only Method Verification Tests

    @Test
    fun `GIVEN vararg-only method WHEN called with different args THEN verifies call count`() {
        // GIVEN
        val fake = fakeTrackedService {
            varargsMethod { it.size }
        }

        // WHEN
        fake.varargsMethod("a")
        fake.varargsMethod("a", "b", "c")
        fake.varargsMethod() // empty varargs

        // THEN
        fake.verifyVarargsMethod {
            assertTrue(wasCalledTimes(3))
            assertFalse(wasNeverCalled())
        }
    }

    @Test
    fun `GIVEN vararg-only method WHEN never called THEN wasNeverCalled returns true`() {
        // GIVEN
        val fake = fakeTrackedService {
            varargsMethod { 0 }
        }

        // WHEN - not calling the method

        // THEN
        fake.verifyVarargsMethod {
            assertTrue(wasNeverCalled())
            assertTrue(wasCalledTimes(0))
        }
    }

    // endregion

    // region Generic Method Verification Tests

    @Test
    fun `GIVEN generic method WHEN called with different types THEN verifies all calls`() {
        // GIVEN
        val fake = fakeTrackedService {
            genericMethod<Any?> { it }
        }

        // WHEN
        fake.genericMethod("string")
        fake.genericMethod(42)
        fake.genericMethod(listOf(1, 2, 3))

        // THEN
        fake.verifyGenericMethod {
            assertTrue(wasCalledTimes(3))
            assertTrue(wasCalledWith("string"))
            assertTrue(wasCalledWith(42))
            assertTrue(wasCalledWith(listOf(1, 2, 3)))
            assertFalse(wasNeverCalled())
        }
    }

    @Test
    fun `GIVEN async generic method WHEN called THEN verifies with type erasure`() {
        // GIVEN
        val fake = fakeTrackedService {
            asyncGenericMethod<Any?> { it }
        }

        // WHEN
        kotlinx.coroutines.test.runTest {
            fake.asyncGenericMethod("test")
            fake.asyncGenericMethod(123)
        }

        // THEN
        fake.verifyAsyncGenericMethod {
            assertTrue(wasCalledTimes(2))
            assertTrue(wasCalledWith("test"))
            assertTrue(wasCalledWith(123))
        }
    }

    // endregion

    // region Mixed Verification Tests

    @Test
    fun `GIVEN multiple methods WHEN each called differently THEN each verifier is independent`() {
        // GIVEN
        val fake = fakeTrackedService {
            simpleMethod { "simple" }
            methodWithParams { _, _ -> true }
            varargsMethod { it.size }
        }

        // WHEN
        fake.simpleMethod()
        fake.simpleMethod()
        fake.methodWithParams("id", 1)
        fake.varargsMethod("a", "b")
        fake.varargsMethod("c")
        fake.varargsMethod("d")

        // THEN - each verifier has independent state
        fake.verifySimpleMethod {
            assertTrue(wasCalledTimes(2))
        }

        fake.verifyMethodWithParams {
            assertTrue(wasCalledTimes(1))
            assertTrue(wasCalledWith("id", 1))
        }

        fake.verifyVarargsMethod {
            assertTrue(wasCalledTimes(3))
        }
    }

    @Test
    fun `GIVEN multiple fake instances WHEN verifying THEN no cross-contamination`() {
        // GIVEN
        val fake1 = fakeTrackedService { simpleMethod { "fake1" } }
        val fake2 = fakeTrackedService { simpleMethod { "fake2" } }

        // WHEN
        fake1.simpleMethod()
        fake1.simpleMethod()
        fake2.simpleMethod()

        // THEN
        fake1.verifySimpleMethod {
            assertTrue(wasCalledTimes(2))
        }

        fake2.verifySimpleMethod {
            assertTrue(wasCalledTimes(1))
        }
    }

    // endregion
}
