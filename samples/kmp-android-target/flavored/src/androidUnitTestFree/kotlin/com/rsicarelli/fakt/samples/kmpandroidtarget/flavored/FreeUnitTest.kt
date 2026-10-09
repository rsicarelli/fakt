// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.flavored

import kotlin.test.Test
import kotlin.test.assertEquals

class FreeUnitTest {
    @Test
    fun `GIVEN free tier fake WHEN reading the item limit THEN returns the configured value`() {
        // Given
        val features = fakeTierFeatures { maxItems { 3 } }
        val gate = fakeFeatureGate { describe { "free" } }

        // When
        val max = features.maxItems()

        // Then
        assertEquals(3, max)
        assertEquals("free", gate.describe())
    }
}
