// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import kotlin.test.Test
import kotlin.test.assertEquals

/** Uses the desktop-only fake (from `faktGenerateDesktopMain`) next to the common one. */
class WindowManagerTest {

    @Test
    fun `GIVEN the desktop target WHEN asking the platform name THEN it is desktop`() {
        // Given / When
        val name = platformName()

        // Then
        assertEquals("desktop", name)
    }

    @Test
    fun `GIVEN a window manager fake WHEN opening a window THEN returns the configured id`() {
        // Given
        val windows = fakeWindowManager { open { _ -> 7 } }

        // When
        val id = windows.open("Main")

        // Then
        assertEquals(7, id)
        assertEquals(listOf("Main"), windows.openCalls.value.map { it.title })
    }
}
