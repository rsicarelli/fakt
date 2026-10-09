// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidUnitTest {
    @Test
    fun `GIVEN androidMain fake WHEN reading its label THEN returns the configured value`() {
        // Given
        val fake = fakeAndroidOnly { label { "android-only" } }

        // When
        val label = fake.label

        // Then
        assertEquals("android-only", label)
    }

    @Test
    fun `GIVEN commonMain fake WHEN running on Android THEN the common fake is available`() {
        // Given
        val repository = fakeUserRepository { findName { "android-user" } }

        // When
        val name = repository.findName("1")

        // Then
        assertEquals("android-user", name)
        assertEquals("android", platformName())
    }
}
