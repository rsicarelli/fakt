// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/**
 * Uses the fake of a `mingwMain` interface. It lives in `mingwX64Test`, the compilation that gets
 * the generated directory: the `mingwTest` fragment is its parent and cannot see it. It compiles
 * on every host (the compile task needs only the Kotlin/Native distribution) and runs only on
 * Windows.
 */
class MingwOnlyServiceTest {
    @Test
    fun `GIVEN MingwOnlyService fake WHEN reading a registry value THEN returns the configured value`() =
        runTest {
            // Given
            val fake = fakeMingwOnlyService {
                readRegistryValue { key -> "value-of-$key" }
            }

            // When
            val value = fake.readRegistryValue("HKLM")

            // Then
            assertEquals("value-of-HKLM", value)
            assertEquals(1, fake.readRegistryValueCalls.value.size)
        }

    @Test
    fun `GIVEN MingwOnlyService fake WHEN using defaults THEN windowsDirectory is empty`() {
        // Given
        val fake = fakeMingwOnlyService()

        // When
        val directory = fake.windowsDirectory()

        // Then
        assertEquals("", directory)
    }
}
