// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class UserRepositoryCommonTest {
    @Test
    fun `GIVEN common fake WHEN finding a name THEN returns the configured value`() {
        // Given
        val repository = fakeUserRepository { findName { id -> "user-$id" } }

        // When
        val name = repository.findName("1")

        // Then
        assertEquals("user-1", name)
    }
}
