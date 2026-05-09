// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core.edgecases

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EdgeCaseNestedFakesTest {
    @Test
    fun `GIVEN deeply nested fake WHEN configured THEN works against fully qualified type`() {
        val fake: L1.L2.DeepService = fakeDeepService { deep { "deep-value" } }

        assertEquals("deep-value", fake.deep())
    }

    @Test
    fun `GIVEN fake nested in companion object WHEN configured THEN works`() {
        val fake: CompanionContainer.Companion.CompanionService = fakeCompanionService {
            companionFn { 7 }
        }

        assertEquals(7, fake.companionFn())
    }

    @Test
    fun `GIVEN fake nested in interface WHEN configured THEN works`() {
        val fake: OuterInterface.InnerService = fakeInnerService { inner { "from-interface" } }

        assertEquals("from-interface", fake.inner())
    }

    @Test
    fun `GIVEN generic fake nested in object WHEN configured THEN preserves type parameter`() {
        val fake: GenericHolder.GenericRepo<String> =
            fakeGenericRepo<String> { get { _ -> "value" } }

        assertEquals("value", fake.get("any"))
    }

    @Test
    fun `GIVEN abstract class fake nested in object WHEN configured THEN works`() {
        val fake: AbstractHolder.NestedAbstractRepo = fakeNestedAbstractRepo {
            fetch { "fetched" }
        }

        assertEquals("fetched", fake.fetch())
    }

    @Test
    fun `GIVEN fake with same simple name as outer object WHEN configured THEN works`() {
        val fake: NameSame.NameSame = fakeNameSame { work { "ok" } }

        assertEquals("ok", fake.work())
    }
}
