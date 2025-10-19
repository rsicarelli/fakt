// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.companionObjects.basic

import com.rsicarelli.fakt.samples.singlemodule.scenarios.companionObjects.basic.User
import com.rsicarelli.fakt.samples.singlemodule.scenarios.companionObjects.basic.UserRepository
import com.rsicarelli.fakt.samples.singlemodule.scenarios.companionObjects.basic.fakeUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for UserRepository interface.
 * Note: Companion object support is a future feature
 */
class UserRepositoryTest {
    @Test
    fun `GIVEN UserRepository fake WHEN configuring getUser THEN should return configured user`() {
        // Given
        val repo =
            fakeUserRepository {
                getUser { id ->
                    Result.success(User(id, "Test User"))
                }
            }

        // When
        val result = repo.getUser("123")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Test User", result.getOrNull()?.name)
        assertEquals("123", result.getOrNull()?.id)
    }

    @Test
    fun `GIVEN UserRepository fake WHEN getUser fails THEN should return failure`() {
        // Given
        val repo =
            fakeUserRepository {
                getUser {
                    Result.failure(Exception("User not found"))
                }
            }

        // When
        val result = repo.getUser("456")

        // Then
        assertTrue(result.isFailure)
    }
}
