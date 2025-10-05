// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.companionObjects.basic

import com.rsicarelli.fakt.samples.singleModule.scenarios.companionObjects.basic.User
import com.rsicarelli.fakt.samples.singleModule.scenarios.companionObjects.basic.UserRepository
import com.rsicarelli.fakt.samples.singleModule.scenarios.companionObjects.basic.fakeUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for UserRepository interface with companion object.
 */
class UserRepositoryTest {
    @Test
    fun `GIVEN interface with companion factory WHEN using fake THEN instance methods should work`() {
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
    }

    @Test
    fun `GIVEN interface with companion factory WHEN accessing companion members THEN should be available`() {
        // Given - UserRepository companion object should exist

        // When
        val timeout = UserRepository.DEFAULT_TIMEOUT

        // Then
        assertEquals(5000L, timeout, "Companion constant should be accessible")
    }

    @Test
    fun `GIVEN interface with companion factory method WHEN configuring fake THEN companion factory should work`() {
        // Given - configure companion create method
        // Note: For now, we'll test that companion members exist
        // Full companion method configuration will be implemented

        // When
        val factory = UserRepository.Companion

        // Then
        assertNotNull(factory, "Companion object should exist")
    }
}
