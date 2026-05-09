// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NestedFakesTest {
    @Test
    fun `GIVEN nested Alpha fake WHEN configured THEN returns configured value`() {
        val fake = fakeAlphaService { alpha { "alpha-ok" } }

        assertEquals("alpha-ok", fake.alpha())
    }

    @Test
    fun `GIVEN nested Beta fake WHEN configured THEN returns configured value`() {
        val fake = fakeBetaService { beta { 42 } }

        assertEquals(42, fake.beta())
    }

    @Test
    fun `GIVEN nested Gamma fake WHEN configured THEN returns configured value`() {
        val fake = fakeGammaService { gamma { true } }

        assertTrue(fake.gamma())
    }

    @Test
    fun `GIVEN nested Delta fake WHEN configured THEN returns configured value`() {
        val fake = fakeDeltaService { delta { 99L } }

        assertEquals(99L, fake.delta())
    }

    @Test
    fun `GIVEN fake of interface nested in class WHEN configured THEN returns configured value`() {
        val fake = fakeNestedInClassService { ping { "pong" } }

        assertEquals("pong", fake.ping())
    }

    @Test
    fun `GIVEN nested fake WHEN implementing parent type THEN compiles against qualified name`() {
        val alpha: NestedFakes.AlphaService = fakeAlphaService { alpha { "polymorphic" } }
        val nested: NestedFakesContainer.NestedInClassService =
            fakeNestedInClassService { ping { "polymorphic" } }

        assertEquals("polymorphic", alpha.alpha())
        assertEquals("polymorphic", nested.ping())
    }
}
