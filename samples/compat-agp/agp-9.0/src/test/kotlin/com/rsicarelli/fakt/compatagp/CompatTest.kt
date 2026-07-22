// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compatagp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Runs as the Android module's unit test (`testDebugUnitTest`). It consumes `fakeUserRepository`,
 * which Fakt generated into this module's Android `testFixtures` source set — proving generation,
 * per-variant Kotlin compilation, and same-module consumption all work on AGP 9.0.
 *
 * Uses JUnit4 (not `kotlin.test`): AGP 9.0's built-in Kotlin doesn't wire `kotlin-test` onto the
 * unit-test classpath like the applied kotlin-android plugin does on the 8.x cells.
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
