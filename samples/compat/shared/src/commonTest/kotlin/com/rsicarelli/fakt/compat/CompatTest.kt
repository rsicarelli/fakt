// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.compat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class CompatTest {

    @Test
    fun `GIVEN fake user repository WHEN using defaults THEN compiles and runs`() {
        val fake = fakeUserRepository {}
        assertNotNull(fake)
        assertNotNull(fake.currentUser)
    }

    @Test
    fun `GIVEN fake user repository WHEN customizing behavior THEN applies correctly`() {
        val fake = fakeUserRepository {
            currentUser { "Alice" }
            getUser { id -> "User-$id" }
            findUsers { _, _ -> listOf("Alice", "Bob") }
        }
        assertEquals("Alice", fake.currentUser)
        assertEquals("User-42", fake.getUser(42))
        assertEquals(listOf("Alice", "Bob"), fake.findUsers("test", 10))
    }

    @Test
    fun `GIVEN fake user repository WHEN calling suspend function THEN works with coroutines`() =
        runTest {
            val fake = fakeUserRepository {
                fetchUser { id -> "AsyncUser-$id" }
            }
            assertEquals("AsyncUser-1", fake.fetchUser(1))
        }

    @Test
    fun `GIVEN generic cache WHEN using fake THEN handles type parameters`() {
        val fake = fakeCache<String> {
            get { key -> "value-$key" }
            getAll { listOf("a", "b", "c") }
        }
        assertEquals("value-myKey", fake.get("myKey"))
        assertEquals(listOf("a", "b", "c"), fake.getAll())
    }

    @Test
    fun `GIVEN notification service WHEN using defaults THEN returns smart defaults`() {
        val fake = fakeNotificationService {}
        assertNotNull(fake)
        assertEquals(false, fake.isEnabled)
        assertEquals(0, fake.getUnreadCount())
    }
}
