// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class ReleaseUnitTest {
    @Test
    fun `GIVEN release BuildFlags WHEN reading the flag fake THEN exposes the release members`() {
        // Given
        val reader = fakeFlagReader { read { BuildFlags() } }

        // When
        val flags = reader.read()

        // Then
        assertTrue(flags.minified)
    }
}
