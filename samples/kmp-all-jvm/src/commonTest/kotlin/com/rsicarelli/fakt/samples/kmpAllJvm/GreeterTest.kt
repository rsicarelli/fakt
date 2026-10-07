// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/**
 * Uses the commonMain fake in commonTest. Before issue #160 an all-JVM project had no commonMain
 * producer, so `fakeUserRepository` was an unresolved reference and this file did not compile.
 */
class GreeterTest {

    @Test
    fun `GIVEN a stored user WHEN greeting THEN uses the user name`() = runTest {
        // Given
        val repository = fakeUserRepository { find { id -> User(id, "Ada") } }

        // When
        val greeting = Greeter(repository).greet("u-1")

        // Then
        assertEquals("Hello, Ada from ${platformName()}", greeting)
    }

    @Test
    fun `GIVEN no stored user WHEN greeting THEN falls back to the current user name`() = runTest {
        // Given
        val repository = fakeUserRepository {
            currentUserName { "Grace" }
            find { null }
        }

        // When
        val greeting = Greeter(repository).greet("missing")

        // Then
        assertEquals("Hello, Grace from ${platformName()}", greeting)
    }
}
