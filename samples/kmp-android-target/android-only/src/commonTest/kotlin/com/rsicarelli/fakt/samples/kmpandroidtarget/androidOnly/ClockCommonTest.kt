// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.androidOnly

import kotlin.test.Test
import kotlin.test.assertEquals

class ClockCommonTest {
    @Test
    fun `GIVEN commonMain fake in a single-target Android module WHEN reading the time THEN returns the configured value`() {
        // Given
        val clock = fakeClock { now { 7L } }

        // When
        val now = clock.now()

        // Then
        assertEquals(7L, now)
    }
}
