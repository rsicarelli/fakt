// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.edgeCases

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserServiceTest {
    @Test
    fun `GIVEN unconfigured findById WHEN called with null THEN returns null`() {
        val service: UserService = fakeUserService {}

        val result = service.findById(null)

        assertNull(result)
    }

    @Test
    fun `GIVEN unconfigured findById WHEN called with id THEN returns null`() {
        val service: UserService = fakeUserService {}

        val result = service.findById("123")

        assertNull(result)
    }

    @Test
    fun `GIVEN configured findById WHEN called with null THEN uses custom behavior`() {
        val service: UserService = fakeUserService {
            findById { id ->
                if (id == null) null else User(id, "Test")
            }
        }

        val result = service.findById(null)

        assertNull(result)
    }

    @Test
    fun `GIVEN configured findById WHEN called with id THEN returns user`() {
        val service: UserService = fakeUserService {
            findById { id ->
                if (id == null) null else User(id, "Test")
            }
        }

        val result = service.findById("123")

        assertNotNull(result)
        assertEquals("123", result.id)
        assertEquals("Test", result.name)
    }

    @Test
    fun `GIVEN unconfigured update WHEN called with null THEN returns false`() {
        val service: UserService = fakeUserService {}

        val result = service.update(null)

        assertFalse(result)
    }

    @Test
    fun `GIVEN configured update WHEN called with null THEN uses custom behavior`() {
        val service: UserService = fakeUserService {
            update { user ->
                user != null
            }
        }

        val result = service.update(null)

        assertFalse(result)
    }

    @Test
    fun `GIVEN configured update WHEN called with user THEN returns true`() {
        val service: UserService = fakeUserService {
            update { user ->
                user != null
            }
        }

        val result = service.update(User("123", "Test"))

        assertTrue(result)
    }

    @Test
    fun `GIVEN unconfigured merge WHEN called THEN returns primary`() {
        val service: UserService = fakeUserService {}
        val primary = User("1", "Primary")

        val result = service.merge(primary, null)

        assertEquals(primary, result)
    }

    @Test
    fun `GIVEN configured merge WHEN both null THEN returns null`() {
        val service: UserService = fakeUserService {
            merge { primary, secondary ->
                primary ?: secondary
            }
        }

        val result = service.merge(null, null)

        assertNull(result)
    }

    @Test
    fun `GIVEN configured merge WHEN primary null THEN returns secondary`() {
        val service: UserService = fakeUserService {
            merge { primary, secondary ->
                primary ?: secondary
            }
        }
        val secondary = User("2", "Secondary")

        val result = service.merge(null, secondary)

        assertEquals(secondary, result)
    }

    @Test
    fun `GIVEN unconfigured currentUser WHEN accessed THEN returns null`() {
        val service: UserService = fakeUserService {}

        val result = service.currentUser

        assertNull(result)
    }

    @Test
    fun `GIVEN configured currentUser WHEN accessed THEN returns user`() {
        val user = User("123", "Current")

        val service: UserService = fakeUserService {
            currentUser { user }
        }

        val result = service.currentUser

        assertEquals(user, result)
    }

    @Test
    fun `GIVEN unconfigured cachedUser WHEN get THEN returns null`() {
        val service: UserService = fakeUserService {}

        val result = service.cachedUser

        assertNull(result)
    }

    @Test
    fun `GIVEN configured cachedUser WHEN set and get THEN uses custom behavior`() {
        var storage: User? = null

        val service: UserService = fakeUserService {
            cachedUser { storage }
            setCachedUser { user -> storage = user }
        }

        val user = User("123", "Cached")
        service.cachedUser = user
        val result = service.cachedUser

        assertEquals(user, result)
    }

    @Test
    fun `GIVEN all methods configured WHEN called THEN all use custom behaviors`() {
        val testUser = User("123", "Test")

        val service: UserService = fakeUserService {
            findById { id -> if (id != null) testUser else null }
            update { user -> user != null }
            merge { primary, secondary -> primary ?: secondary }
            currentUser { testUser }
        }

        val found = service.findById("123")
        val updated = service.update(testUser)
        val merged = service.merge(null, testUser)
        val current = service.currentUser

        assertEquals(testUser, found)
        assertTrue(updated)
        assertEquals(testUser, merged)
        assertEquals(testUser, current)
    }
}
