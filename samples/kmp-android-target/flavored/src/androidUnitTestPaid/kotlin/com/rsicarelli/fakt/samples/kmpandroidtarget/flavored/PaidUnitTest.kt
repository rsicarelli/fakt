// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.flavored

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaidUnitTest {
    @Test
    fun `GIVEN paid tier fake WHEN reading its members THEN returns the configured values`() {
        // Given
        val features =
            fakeTierFeatures {
                maxItems { 100 }
                exportEnabled { true }
            }
        val gate = fakeFeatureGate { describe { "paid" } }

        // When
        val max = features.maxItems()

        // Then
        assertEquals(100, max)
        assertTrue(features.exportEnabled())
        assertEquals("paid", gate.describe())
    }
}
