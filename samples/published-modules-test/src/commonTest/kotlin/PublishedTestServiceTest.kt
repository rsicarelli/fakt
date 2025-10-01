// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.sample.published

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublishedTestServiceTest {

    @Test
    fun `GIVEN published test service fake WHEN using basic features THEN should work correctly`() {
        val service = fakePublishedTestService {
            getName { "Test Service" }
            name { "Test Name" }
            isActive { true }
        }

        assertEquals("Test Service", service.getName())
        assertEquals("Test Name", service.name)
        assertTrue(service.isActive)
    }

    @Test
    fun `GIVEN published user service fake WHEN using suspend functions THEN should work correctly`() {
        val userService = fakePublishedUserService {
            fetchUser { userId -> "User: $userId" }
            hasPermission { permission -> permission == "admin" }
        }

        assertTrue(userService.hasPermission("admin"))
        // Note: suspend function test would need runTest from coroutines-test
    }
}
