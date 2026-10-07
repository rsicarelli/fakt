// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import kotlin.test.Test
import kotlin.test.assertEquals

/** Uses the server-only fake (from `faktGenerateServerMain`). */
class RequestLogTest {

    @Test
    fun `GIVEN the server target WHEN asking the platform name THEN it is server`() {
        // Given / When
        val name = platformName()

        // Then
        assertEquals("server", name)
    }

    @Test
    fun `GIVEN a request log fake WHEN recording a path THEN the call is tracked`() {
        // Given
        val log = fakeRequestLog()

        // When
        log.record("/health")

        // Then
        assertEquals(listOf("/health"), log.recordCalls.value.map { it.path })
    }
}
