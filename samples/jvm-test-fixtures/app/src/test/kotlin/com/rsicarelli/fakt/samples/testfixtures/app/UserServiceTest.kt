// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.app

import com.rsicarelli.fakt.samples.testfixtures.core.fakeCacheManager
import com.rsicarelli.fakt.samples.testfixtures.core.fakeUserRepository
import com.rsicarelli.fakt.samples.testfixtures.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [UserService] using fakes from core's testFixtures.
 *
 * This demonstrates the key benefit of Gradle test fixtures with Fakt:
 * fakes generated in the core module are directly available in this
 * module's tests via `testImplementation(testFixtures(projects.core))`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {
    @Test
    fun `GIVEN user exists WHEN getUser THEN returns user from repository`() {
        val userRepo = fakeUserRepository {
            findById { id -> User(id, "Alice", "alice@test.com") }
        }
        val cache = fakeCacheManager()
        val service = UserService(userRepo, cache)

        val result = service.getUser("1")

        assertEquals("Alice", result?.name)
    }

    @Test
    fun `GIVEN cache hit WHEN getUser THEN skips repository lookup`() {
        val userRepo = fakeUserRepository {
            findById { id -> User(id, "Alice") }
        }
        val cache = fakeCacheManager {
            get { "cached-value" }
        }
        val service = UserService(userRepo, cache)

        val result = service.getUser("1")

        assertNull(result)
    }

    @Test
    fun `GIVEN users exist WHEN getAllUsers THEN returns all users`() {
        val users = listOf(User("1", "Alice"), User("2", "Bob"))
        val userRepo = fakeUserRepository {
            findAll { users }
        }
        val service = UserService(userRepo, fakeCacheManager())

        val result = service.getAllUsers()

        assertEquals(2, result.size)
        assertEquals("Alice", result[0].name)
        assertEquals("Bob", result[1].name)
    }

    @Test
    fun `GIVEN valid data WHEN createUser THEN saves and caches user`() {
        val cached = mutableMapOf<String, String>()
        val userRepo = fakeUserRepository {
            save { user -> user.copy(id = "generated-id") }
        }
        val cache = fakeCacheManager {
            put { key, value -> cached[key] = value }
        }
        val service = UserService(userRepo, cache)

        val result = service.createUser("Alice", "alice@test.com")

        assertEquals("generated-id", result.id)
        assertEquals("Alice", cached["user:generated-id"])
    }

    @Test
    fun `GIVEN user exists WHEN deleteUser THEN removes from repo and cache`() {
        val invalidated = mutableListOf<String>()
        val userRepo = fakeUserRepository {
            delete { true }
        }
        val cache = fakeCacheManager {
            invalidate { key -> invalidated.add(key) }
        }
        val service = UserService(userRepo, cache)

        val result = service.deleteUser("1")

        assertTrue(result)
        assertEquals(listOf("user:1"), invalidated)
    }

    @Test
    fun `GIVEN user does not exist WHEN deleteUser THEN does not invalidate cache`() {
        val invalidated = mutableListOf<String>()
        val userRepo = fakeUserRepository {
            delete { false }
        }
        val cache = fakeCacheManager {
            invalidate { key -> invalidated.add(key) }
        }
        val service = UserService(userRepo, cache)

        val result = service.deleteUser("1")

        assertEquals(false, result)
        assertTrue(invalidated.isEmpty())
    }
}
