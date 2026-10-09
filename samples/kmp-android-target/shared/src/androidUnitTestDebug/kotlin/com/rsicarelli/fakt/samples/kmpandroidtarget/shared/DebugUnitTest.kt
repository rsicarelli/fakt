// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugUnitTest {
    @Test
    fun `GIVEN debug fake WHEN listing menu items THEN returns the configured items`() {
        // Given
        val menu = fakeDebugMenu { items { listOf("logs", "network") } }

        // When
        val items = menu.items()

        // Then
        assertEquals(listOf("logs", "network"), items)
    }

    @Test
    fun `GIVEN debug BuildFlags WHEN reading the flag fake THEN exposes the debug members`() {
        // Given
        val reader = fakeFlagReader { read { BuildFlags() } }

        // When
        val flags = reader.read()

        // Then
        assertTrue(flags.verboseLogging)
    }
}
