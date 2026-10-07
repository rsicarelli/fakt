// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import kotlin.test.Test
import kotlin.test.assertEquals

class CliTest {

    @Test
    fun `GIVEN the cli target WHEN asking the transport THEN it is stdio`() {
        // Given / When
        val transport = transport()

        // Then
        assertEquals("stdio", transport)
    }

    @Test
    fun `GIVEN the cli target WHEN greeting with a commonMain fake THEN the platform is cli`() {
        // Given
        val repository = fakeUserRepository { currentUserName { "Linus" } }

        // When
        val name = repository.currentUserName

        // Then
        assertEquals("Linus", name)
        assertEquals("cli", platformName())
    }
}
