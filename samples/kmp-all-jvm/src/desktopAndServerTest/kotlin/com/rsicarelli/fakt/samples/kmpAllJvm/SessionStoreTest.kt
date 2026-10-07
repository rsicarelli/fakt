// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionStoreTest {

    @Test
    fun `GIVEN a session store fake WHEN opening a session THEN returns the configured token`() {
        // Given
        val store = fakeSessionStore { open { user -> "token-${user.id}" } }

        // When
        val token = store.open(User("u-1", "Ada"))

        // Then
        assertEquals("token-u-1", token)
        assertEquals(1, store.openCalls.value.size)
    }

    @Test
    fun `GIVEN the desktop and server targets WHEN asking the transport THEN the shared actual answers`() {
        // Given / When
        val transport = transport()

        // Then
        assertEquals("gui-or-http", transport)
    }
}
