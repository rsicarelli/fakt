// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.companionObjects.basic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRepositoryTest {
    @Test
    fun `GIVEN UserRepository fake WHEN configuring getUser THEN should return configured user`() {
        // Given
        val repo = fakeUserRepository {
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
        val repo = fakeUserRepository {
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
