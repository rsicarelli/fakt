// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.overloads

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Regression tests for #146: overloaded methods get distinct generated members. */
class OverloadedMethodsTest {
    @Test
    fun `GIVEN overloaded find WHEN each overload configured THEN each uses its own behavior`() {
        // Given
        val service = fakeOverloadedService {
            findInt { id -> "id:$id" }
            findString { name -> "name:$name" }
            find { listOf("all") }
        }

        // When
        val byId = service.find(1)
        val byName = service.find("fakt")
        val all = service.find()

        // Then
        assertEquals("id:1", byId)
        assertEquals("name:fakt", byName)
        assertEquals(listOf("all"), all)
    }

    @Test
    fun `GIVEN overloaded setLocalOverride WHEN called THEN records calls per overload`() {
        // Given
        val toggle = Toggle("dark-mode")
        val abTest = ABTest<Variant>("checkout")
        val service = fakeOverloadedService()

        // When
        service.setLocalOverride(toggle, true)
        service.setLocalOverride(abTest, "TREATMENT")
        service.setLocalOverride(abTest, "CONTROL")

        // Then
        service.verifySetLocalOverrideFeatureAny {
            assertTrue(wasCalledTimes(1))
            assertTrue(wasCalledWith(toggle, true))
        }
        service.verifySetLocalOverrideABTestString {
            assertTrue(wasCalledTimes(2))
            assertEquals("CONTROL", lastOrNull?.override)
        }
    }

    @Test
    fun `GIVEN overloaded suspend load WHEN configured THEN each overload returns its value`() =
        runTest {
            // Given
            val service = fakeOverloadedService {
                loadInt { id -> "item-$id" }
                loadList { ids -> ids.map { "item-$it" } }
            }

            // When
            val single = service.load(1)
            val many = service.load(listOf(1, 2))

            // Then
            assertEquals("item-1", single)
            assertEquals(listOf("item-1", "item-2"), many)
        }

    @Test
    fun `GIVEN non-overloaded method WHEN configured THEN keeps its plain name`() {
        // Given
        val service = fakeOverloadedService { unique { 7 } }

        // When
        val result = service.unique()

        // Then
        assertEquals(7, result)
    }

    @Test
    fun `GIVEN abstract class with overloaded abstract methods WHEN unconfigured THEN fails`() {
        // Given
        val repo = fakeOverloadedRepo()

        // When / Then
        val error =
            assertFailsWith<IllegalStateException> {
                repo.setLocalOverride(ABTest<Variant>("checkout"), "TREATMENT")
            }
        assertTrue(error.message.orEmpty().contains("setLocalOverrideABTestString"))
    }

    @Test
    fun `GIVEN abstract class with overloaded open methods WHEN unconfigured THEN uses super`() {
        // Given
        val repo = fakeOverloadedRepo()

        // When
        val none = repo.count()
        val prefixed = repo.count("abc")

        // Then
        assertEquals(0, none)
        assertEquals(3, prefixed)
    }

    @Test
    fun `GIVEN mutable abstract class WHEN one overload modified THEN other overload unchanged`() {
        // Given
        val repo = fakeOverloadedRepo { count { 1 } }

        // When
        repo.modify { countString { prefix -> prefix.length * 10 } }

        // Then
        assertEquals(1, repo.count())
        assertEquals(30, repo.count("abc"))
    }
}
