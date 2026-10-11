// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmTest {
    @Test
    fun `GIVEN jvm fake WHEN reading the pid THEN returns the configured value`() {
        // Given
        val fake = fakeJvmOnly { pid { 42L } }

        // When
        val pid = fake.pid()

        // Then
        assertEquals(42L, pid)
        assertEquals("jvm", platformName())
    }
}
