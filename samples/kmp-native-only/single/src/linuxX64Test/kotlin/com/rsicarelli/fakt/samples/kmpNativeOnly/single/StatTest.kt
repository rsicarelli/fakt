// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpNativeOnly.single

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatTest {
    @Test
    fun `GIVEN Stat fake WHEN asking statistics with defaults THEN should return null`() {
        // Given
        val fake = fakeStat()

        // When
        val result = fake.statistics("/tmp")

        // Then
        assertNull(result)
        assertEquals(1, fake.statisticsCalls.value.size)
    }

    @Test
    fun `GIVEN Stat fake WHEN checking existence THEN should return the configured answer`() {
        // Given
        val fake = fakeStat { exists { true } }

        // When
        val exists = fake.exists("/tmp")

        // Then
        assertTrue(exists)
        assertEquals(1, fake.existsCalls.value.size)
    }
}
