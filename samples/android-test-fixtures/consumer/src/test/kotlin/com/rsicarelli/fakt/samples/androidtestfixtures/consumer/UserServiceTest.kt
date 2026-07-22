// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.androidtestfixtures.consumer

import com.rsicarelli.fakt.samples.androidtestfixtures.producer.fakeUserRepository
import com.rsicarelli.fakt.samples.androidtestfixtures.producer.models.User
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Consumes the `fakeUserRepository { }` fake generated into the producer's Android `testFixtures`
 * source set and pulled in via `testImplementation(testFixtures(project(":producer")))`.
 *
 * This is the end-to-end proof for discussion #109: an Android (AGP) module using another Android
 * module's Fakt-generated test fixtures, which the old `java-test-fixtures`-only path could not
 * support.
 */
class UserServiceTest {

    @Test
    fun `GIVEN fake user repository from producer test fixtures WHEN displaying a name THEN service resolves it`() {
        // Given — the fake type comes from the producer's testFixtures artifact.
        val repository =
            fakeUserRepository {
                findById { id ->
                    if (id == "1") User("1", "Alice", "alice@example.com") else null
                }
            }
        val service = UserService(repository)

        // When
        val name = service.displayName("1")
        val missing = service.displayName("missing")

        // Then
        assertEquals("Alice", name)
        assertEquals("Unknown", missing)
    }

    @Test
    fun `GIVEN fake user repository with configured users WHEN counting THEN service reports the size`() {
        // Given
        val repository =
            fakeUserRepository {
                users {
                    listOf(
                        User("1", "Alice", "alice@example.com"),
                        User("2", "Bob", "bob@example.com"),
                    )
                }
            }
        val service = UserService(repository)

        // When
        val count = service.registeredCount()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `GIVEN fake user repository with defaults WHEN used THEN smart defaults apply`() {
        // Given — no behavior configured; Fakt supplies smart defaults.
        val service = UserService(fakeUserRepository())

        // When / Then
        assertEquals(0, service.registeredCount())
        assertEquals("Unknown", service.displayName("any"))
    }
}
