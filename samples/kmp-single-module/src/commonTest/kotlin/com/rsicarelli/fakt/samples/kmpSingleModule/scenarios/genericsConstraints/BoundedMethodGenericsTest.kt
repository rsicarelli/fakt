// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsConstraints

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Regression tests for #145: bounded method-level type parameters used as type arguments. */
class BoundedMethodGenericsTest {
    private val checkout = ABTest(key = "checkout", default = Variant.CONTROL)

    @Test
    fun `GIVEN interface with enum-bounded generic WHEN configured THEN returns typed enum`() {
        // Given
        val service = fakeExperimentsService {
            abTestAsEnum { test: ABTest<Variant> -> Variant.valueOf("TREATMENT") }
        }

        // When
        val result: Variant = service.abTestAsEnum(checkout)

        // Then
        assertEquals(Variant.TREATMENT, result)
    }

    @Test
    fun `GIVEN interface with enum-bounded generic WHEN called THEN records call history`() {
        // Given
        val service = fakeExperimentsService {
            abTestAsEnum { test: ABTest<Variant> -> test.default }
        }

        // When
        service.abTestAsEnum(checkout)

        // Then
        service.verifyAbTestAsEnum {
            assertTrue(wasCalledWith(checkout))
            assertEquals(checkout, first.abTest)
        }
    }

    @Test
    fun `GIVEN interface with Any-bounded generic WHEN configured THEN unboxes value`() {
        // Given
        val service = fakeExperimentsService {
            unbox { box: NonNullBox<String> -> box.value.uppercase() }
        }

        // When
        val result: String = service.unbox(NonNullBox("fakt"))

        // Then
        assertEquals("FAKT", result)
    }

    @Test
    fun `GIVEN interface with bounded generic nested in collection WHEN configured THEN maps`() {
        // Given
        val service = fakeExperimentsService {
            allVariants { tests: List<ABTest<Variant>> -> tests.associate { it.key to it.default } }
        }

        // When
        val result: Map<String, Variant> = service.allVariants(listOf(checkout))

        // Then
        assertEquals(mapOf("checkout" to Variant.CONTROL), result)
    }

    @Test
    fun `GIVEN abstract class with enum-bounded generic WHEN configured THEN returns typed enum`() {
        // Given
        val repo = fakeExperimentsRepo {
            abTestAsEnum { test: ABTest<Variant> -> test.default }
            unbox { box: NonNullBox<Int> -> box.value + 1 }
        }

        // When
        val variant: Variant = repo.abTestAsEnum(checkout)
        val unboxed: Int = repo.unbox(NonNullBox(41))

        // Then
        assertEquals(Variant.CONTROL, variant)
        assertEquals(42, unboxed)
    }

    @Test
    fun `GIVEN abstract class with unconfigured bounded generic WHEN called THEN fails`() {
        // Given
        val repo = fakeExperimentsRepo()

        // When / Then
        assertFailsWith<IllegalStateException> { repo.abTestAsEnum(checkout) }
    }

    @Test
    fun `GIVEN abstract class with open bounded generic WHEN not configured THEN uses super`() {
        // Given
        val repo = fakeExperimentsRepo()

        // When
        val enabled = repo.isEnabled(checkout)

        // Then
        assertFalse(enabled)
    }

    @Test
    fun `GIVEN mutable abstract class WHEN reconfigured THEN uses new behavior`() {
        // Given
        val repo = fakeExperimentsRepo { isEnabled { _: ABTest<Variant> -> false } }

        // When
        repo.modify { isEnabled { _: ABTest<Variant> -> true } }

        // Then
        assertTrue(repo.isEnabled(checkout))
    }
}
