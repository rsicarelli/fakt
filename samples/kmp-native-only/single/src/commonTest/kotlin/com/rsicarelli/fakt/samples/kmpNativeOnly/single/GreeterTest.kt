// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpNativeOnly.single

import kotlin.test.Test
import kotlin.test.assertEquals

class GreeterTest {
    @Test
    fun `GIVEN Greeter fake WHEN greeting THEN should return the configured message`() {
        // Given
        val fake = fakeGreeter { greet { name -> "Hello, $name" } }

        // When
        val message = fake.greet("Fakt")

        // Then
        assertEquals("Hello, Fakt", message)
        assertEquals(1, fake.greetCalls.value.size)
    }

    @Test
    fun `GIVEN the linux test run WHEN asking the host family THEN should be linuxX64`() {
        // When
        val family = hostFamily()

        // Then
        assertEquals("linuxX64", family)
    }
}
