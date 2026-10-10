// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.androidOnly

import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryUnitTest {
    @Test
    fun `GIVEN androidMain fake WHEN reading the level THEN returns the configured value`() {
        // Given
        val battery = fakeBattery { level { 80 } }

        // When
        val level = battery.level()

        // Then
        assertEquals(80, level)
        assertEquals("device", deviceName())
    }

    @Test
    fun `GIVEN commonMain fake WHEN used from androidUnitTest THEN the common fake is available`() {
        // Given
        val clock = fakeClock { now { 1L } }

        // When
        val now = clock.now()

        // Then
        assertEquals(1L, now)
    }
}
