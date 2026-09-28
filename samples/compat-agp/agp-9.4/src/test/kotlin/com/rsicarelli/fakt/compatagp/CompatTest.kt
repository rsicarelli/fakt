// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compatagp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Runs as the Android module's unit test (`testDebugUnitTest`). It consumes `fakeUserRepository`,
 * which Fakt generated into this module's Android `testFixtures` source set — proving generation,
 * per-variant Kotlin compilation, and same-module consumption all work on the pinned AGP version.
 */
class CompatTest {

    @Test
    fun `GIVEN fake user repository WHEN using defaults THEN compiles and runs`() {
        val fake = fakeUserRepository {}
        assertNotNull(fake)
        assertNotNull(fake.currentUser)
    }

    @Test
    fun `GIVEN fake user repository WHEN customizing behavior THEN applies correctly`() {
        val fake =
            fakeUserRepository {
                currentUser { "Alice" }
                getUser { id -> "User-$id" }
                findUsers { _, _ -> listOf("Alice", "Bob") }
            }
        assertEquals("Alice", fake.currentUser)
        assertEquals("User-42", fake.getUser(42))
        assertEquals(listOf("Alice", "Bob"), fake.findUsers("test", 10))
    }
}
